package io.autoptu.cobblemon.authority;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** File-backed write-ahead journal for crash-recoverable authoritative post-battle commits. */
public final class FileAuthoritativePostBattleTransactionRepository {
    private static final int MAGIC = 0x41504254; // APBT
    private static final int SCHEMA_VERSION = 1;
    private final Path directory;

    public FileAuthoritativePostBattleTransactionRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("post-battle-transactions");
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create post-battle transaction store", error);
        }
    }

    public synchronized Optional<AuthoritativePostBattleTransaction> find(String reservationId) {
        String id = requireText(reservationId, "reservationId");
        Path path = statePath(id);
        return Files.exists(path) ? Optional.of(read(path)) : Optional.empty();
    }

    public synchronized boolean createIfAbsent(AuthoritativePostBattleTransaction transaction) {
        if (transaction == null) throw new IllegalArgumentException("transaction is required");
        Path path = statePath(transaction.reservationId());
        if (Files.exists(path)) return false;
        writeAtomically(path, transaction);
        return true;
    }

    public synchronized boolean markCommitted(String reservationId) {
        String id = requireText(reservationId, "reservationId");
        Path path = statePath(id);
        if (!Files.exists(path)) return false;
        AuthoritativePostBattleTransaction current = read(path);
        if (current.phase() == AuthoritativePostBattleTransaction.Phase.COMMITTED) return true;
        writeAtomically(path, current.committed());
        return true;
    }

    public synchronized List<AuthoritativePostBattleTransaction> findPending() {
        List<AuthoritativePostBattleTransaction> pending = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.bin")) {
            for (Path path : files) {
                AuthoritativePostBattleTransaction transaction = read(path);
                if (transaction.phase() == AuthoritativePostBattleTransaction.Phase.PREPARED) pending.add(transaction);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to scan post-battle transaction store", error);
        }
        pending.sort((left, right) -> left.reservationId().compareTo(right.reservationId()));
        return List.copyOf(pending);
    }

    private AuthoritativePostBattleTransaction read(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid post-battle transaction magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported post-battle transaction schema");
                String reservationId = input.readUTF();
                String playerId = input.readUTF();
                String digest = input.readUTF();
                AuthoritativePostBattleTransaction.Phase phase = AuthoritativePostBattleTransaction.Phase.values()[input.readUnsignedByte()];

                int consumptionCount = input.readInt();
                LinkedHashMap<String, Integer> consumptions = new LinkedHashMap<>();
                for (int index = 0; index < consumptionCount; index++) consumptions.put(input.readUTF(), input.readInt());

                int pokemonCount = input.readInt();
                List<AuthoritativePostBattlePokemonFinalState> states = new ArrayList<>(pokemonCount);
                for (int index = 0; index < pokemonCount; index++) {
                    String pokemonId = input.readUTF();
                    long revision = input.readLong();
                    CanonicalHealth health = new CanonicalHealth(input.readInt(), input.readInt());
                    int injuries = input.readInt();
                    int statusCount = input.readInt();
                    List<CanonicalStatusEntry> entries = new ArrayList<>(statusCount);
                    for (int statusIndex = 0; statusIndex < statusCount; statusIndex++) {
                        String name = input.readUTF();
                        int payloadCount = input.readInt();
                        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
                        for (int payloadIndex = 0; payloadIndex < payloadCount; payloadIndex++) {
                            payload.put(input.readUTF(), readScalar(input));
                        }
                        entries.add(new CanonicalStatusEntry(name, payload));
                    }
                    states.add(new AuthoritativePostBattlePokemonFinalState(
                            pokemonId, revision, health, new CanonicalStatusState(entries), new CanonicalInjuryState(injuries)));
                }
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing post-battle transaction data");
                return new AuthoritativePostBattleTransaction(
                        reservationId, playerId, digest, consumptions, states, phase);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read post-battle transaction", error);
        }
    }

    private void writeAtomically(Path target, AuthoritativePostBattleTransaction transaction) {
        byte[] encoded = encode(transaction);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(directory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encoded);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("post-battle transaction store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist post-battle transaction", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private byte[] encode(AuthoritativePostBattleTransaction transaction) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(transaction.reservationId());
                output.writeUTF(transaction.playerId());
                output.writeUTF(transaction.engineTranscriptDigest());
                output.writeByte(transaction.phase().ordinal());
                output.writeInt(transaction.consumedItemQuantities().size());
                transaction.consumedItemQuantities().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                    try {
                        output.writeUTF(entry.getKey());
                        output.writeInt(entry.getValue());
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                });
                output.writeInt(transaction.pokemonFinalStates().size());
                for (AuthoritativePostBattlePokemonFinalState state : transaction.pokemonFinalStates()) {
                    output.writeUTF(state.pokemonId());
                    output.writeLong(state.expectedRevision());
                    output.writeInt(state.health().currentHp());
                    output.writeInt(state.health().maxHp());
                    output.writeInt(state.injuryState().injuries());
                    output.writeInt(state.statusState().entries().size());
                    for (CanonicalStatusEntry entry : state.statusState().entries()) {
                        output.writeUTF(entry.name());
                        output.writeInt(entry.payload().size());
                        for (Map.Entry<String, Object> payload : entry.payload().entrySet()) {
                            output.writeUTF(payload.getKey());
                            writeScalar(output, payload.getValue());
                        }
                    }
                }
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode post-battle transaction", error);
        }
    }

    private static void writeScalar(DataOutputStream output, Object value) throws IOException {
        if (value == null) { output.writeByte(0); return; }
        if (value instanceof String string) { output.writeByte(1); output.writeUTF(string); return; }
        if (value instanceof Integer integer) { output.writeByte(2); output.writeInt(integer); return; }
        if (value instanceof Long longValue) { output.writeByte(3); output.writeLong(longValue); return; }
        if (value instanceof Double doubleValue) { output.writeByte(4); output.writeDouble(doubleValue); return; }
        if (value instanceof Boolean booleanValue) { output.writeByte(5); output.writeBoolean(booleanValue); return; }
        throw new IllegalArgumentException("unsupported status payload scalar: " + value.getClass().getName());
    }

    private static Object readScalar(DataInputStream input) throws IOException {
        return switch (input.readUnsignedByte()) {
            case 0 -> null;
            case 1 -> input.readUTF();
            case 2 -> input.readInt();
            case 3 -> input.readLong();
            case 4 -> input.readDouble();
            case 5 -> input.readBoolean();
            default -> throw new IllegalStateException("unsupported status payload scalar tag");
        };
    }

    private Path statePath(String reservationId) {
        return directory.resolve(sha256(reservationId) + ".bin");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder encoded = new StringBuilder(digest.length * 2);
            for (byte current : digest) encoded.append(String.format("%02x", current));
            return encoded.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.strip();
    }
}
