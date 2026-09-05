package io.autoptu.cobblemon.fabric.presentation;

import com.bedrockk.molang.runtime.MoLangRuntime;
import com.bedrockk.molang.runtime.value.DoubleValue;
import com.bedrockk.molang.runtime.value.StringValue;
import com.cobblemon.mod.common.api.molang.MoLangFunctions;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.moves.animations.ActionEffectContext;
import com.cobblemon.mod.common.api.moves.animations.ActionEffectTimeline;
import com.cobblemon.mod.common.api.moves.animations.ActionEffects;
import com.cobblemon.mod.common.api.moves.animations.TargetsProvider;
import com.cobblemon.mod.common.api.moves.animations.UsersProvider;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Presentation-only bridge into Cobblemon's data-driven move ActionEffect timelines.
 *
 * AutoPTU remains the only battle authority. This class does not create or consult a Cobblemon
 * battle, BattleState, BattlePokemon, move legality, RNG, damage, HP, status or result. It only
 * takes an already-authoritative AutoPTU move result and supplies that result as presentation
 * context to a loaded Cobblemon model animation/particle/sound timeline. Exact move assets are
 * preferred; missing assets may reuse a deterministic native substitute selected only for visuals.
 */
final class CobblemonNativeMoveAnimationBridge {
    private CobblemonNativeMoveAnimationBridge() {
    }

    static boolean tryRender(
            PokemonEntity attacker,
            PokemonEntity target,
            String moveId,
            boolean hit,
            int damage
    ) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        if (damage < 0) throw new IllegalArgumentException("damage cannot be negative");
        if (!hit && damage != 0) {
            throw new IllegalArgumentException("authoritative miss cannot contain damage");
        }

        CobblemonMoveAnimationRouting.Route route = CobblemonMoveAnimationRouting.resolve(moveId);
        if (!route.nativeEffect()) {
            return false;
        }

        String effectPath = route.effectPath();
        ActionEffectTimeline effect = ActionEffects.INSTANCE.getActionEffects().get(
                Identifier.of("cobblemon", effectPath)
        );
        if (effect == null) {
            // Resource reloads can theoretically race route selection. Fail back to the project
            // renderer rather than treating a presentation asset as required battle state.
            return false;
        }

        MoLangRuntime runtime = new MoLangRuntime();
        MoLangFunctions.INSTANCE.setup(runtime);

        runtime.getEnvironment().query.addFunction("user", params -> attacker.getStruct());
        runtime.getEnvironment().query.addFunction("target", params -> target.getStruct());
        runtime.getEnvironment().query.addFunction("targets", params -> target.getStruct());
        runtime.getEnvironment().query.addFunction("missed", params -> new DoubleValue(hit ? 0.0D : 1.0D));
        runtime.getEnvironment().query.addFunction("hurt", params -> new DoubleValue(damage > 0 ? 1.0D : 0.0D));
        runtime.getEnvironment().query.addFunction(
                "move",
                params -> Moves.getByNameOrDummy(effectPath).getStruct()
        );
        runtime.getEnvironment().query.addFunction(
                "instruction_id",
                params -> new StringValue("cobblemon:move")
        );

        List<Object> providers = new ArrayList<>();
        providers.add(new UsersProvider(attacker));
        providers.add(new TargetsProvider(target));

        ActionEffectContext context = new ActionEffectContext(
                effect,
                new HashSet<>(),
                providers,
                runtime,
                false,
                false,
                new ArrayList<>(),
                attacker.getWorld()
        );

        // Some Cobblemon timelines temporarily move a presentation entity and then return it. That
        // animation never becomes PTU position truth: AutoPTU coordinates remain canonical and the
        // adapter never reads the temporary Cobblemon transform back into battle state.
        effect.run(context);
        if (route.substituted()) {
            CobblemonSubstituteAnimationAccent.render(attacker, target, moveId, hit, route.variant());
        }
        return true;
    }

    static String effectPath(String moveId) {
        return BattleMoveAnimationProfile.normalize(moveId);
    }
}
