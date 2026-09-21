package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;

import java.util.UUID;

/**
 * Minecraft-native collision steering for already-authoritative Marea ambient presentation.
 *
 * This runtime only adjusts low-speed CALM presentation movement after the ambient controller has
 * selected it. It reads Minecraft collision/navigation geometry, server-owned canonical population
 * bindings and the authored habitat leash. It never supplies PTU movement legality, targets, RNG,
 * combat state or outcomes, and it never reads Cobblemon Pokemon gameplay payload/state.
 */
public final class MareaWildCalmCollisionSteeringRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final double MAX_CALM_SPEED = 0.025001D;
    private static final double MIN_HORIZONTAL_SPEED = 0.000001D;
    private static final double COLLISION_PROBE_DISTANCE = 0.75D;
    private static final double NATIVE_NAVIGATION_SPEED = 0.08D;
    private static final int MAX_CALM_NEIGHBOR_SURFACE_DELTA = 1;
    private static final double[] TURN_ANGLES_DEGREES = {45.0D, 90.0D, 135.0D};
    private static final int[][] CARDINAL_SURFACE_OFFSETS = {
            {0, -1}, {0, 1}, {-1, 0}, {1, 0}
    };

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS != 0) return;
            steer(server.getOverworld());
        });
    }

    static void steer(ServerWorld world) {
        if (world == null) return;
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            var projectedSiteId = WildEcologyDescriptorRegistry.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) continue;
            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) continue;
                var loaded = world.getEntity(boundUuid.get());
                if (!(loaded instanceof PokemonEntity actor) || actor.isRemoved() || actor.isInvisible()) continue;
                if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;
                var velocity = actor.getVelocity();
                double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                if (speed <= MIN_HORIZONTAL_SPEED || speed > MAX_CALM_SPEED) continue;
                BlockPos anchor = WildPopulationRuntime.projectedPresentationAnchor(encounter, projectedSiteId.get());
                double centerX = anchor.getX() + 0.5D;
                double centerZ = anchor.getZ() + 0.5D;
                if (isPresentationProbeClear(world, actor, velocity.x, velocity.z)) continue;
                double[] target = MareaWildAmbientBehaviorRuntime.calmRoamingTarget(actor.getUuid(), world.getTime(), centerX, centerZ, population.habitatLeashRadiusBlocks());
                if (startNativeNavigation(actor, centerX, centerZ, population.habitatLeashRadiusBlocks(), target)) {
                    actor.setVelocity(0.0D, velocity.y, 0.0D); actor.velocityModified = true; continue;
                }
                double[] safe = firstCollisionFreeVelocity(world, actor, centerX, centerZ, population.habitatLeashRadiusBlocks(), velocity.x, velocity.z);
                actor.setVelocity(safe[0], velocity.y, safe[1]); actor.velocityModified = true;
                if (Math.abs(safe[0]) > MIN_HORIZONTAL_SPEED || Math.abs(safe[1]) > MIN_HORIZONTAL_SPEED) actor.setYaw((float) Math.toDegrees(Math.atan2(-safe[0], safe[1])));
            }
        }
    }

    private static boolean startNativeNavigation(PokemonEntity actor,double centerX,double centerZ,int leashRadiusBlocks,double[] target) { Path path=findLeashSafeNativePath(actor,centerX,centerZ,leashRadiusBlocks,target); return path!=null&&actor.getNavigation().startMovingAlong(path,NATIVE_NAVIGATION_SPEED); }
    static Path findLeashSafeNativePath(PokemonEntity actor,double centerX,double centerZ,int leashRadiusBlocks,double[] target) {
        if(actor==null) throw new IllegalArgumentException("actor is required");
        if(target==null||target.length<2||!Double.isFinite(target[0])||!Double.isFinite(target[1])) throw new IllegalArgumentException("native navigation target requires finite X/Z");
        if(!navigationTargetInsideLeash(centerX,centerZ,leashRadiusBlocks,actor.getX(),actor.getZ())||!navigationTargetInsideLeash(centerX,centerZ,leashRadiusBlocks,target[0],target[1])||!(actor.getWorld() instanceof ServerWorld world)) return null;
        int targetX=MathHelper.floor(target[0]),targetZ=MathHelper.floor(target[1]),actorY=MathHelper.floor(actor.getY()); int surfaceY=world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,targetX,targetZ); if(!stableCalmTargetSurface(world,targetX,targetZ,surfaceY)) return null;
        var navigation=actor.getNavigation(); for(int targetY:navigationTargetYCandidates(actorY,surfaceY)){Path path=navigation.findPathTo(target[0],targetY,target[1],0); if(path==null||!path.reachesTarget()||!navigationPathInsideLeash(centerX,centerZ,leashRadiusBlocks,path)||!navigationPathSurfaceContinuous(world,path)||!navigationPathPresentationClear(world,actor,path)) continue; return path;} return null;
    }
    private static boolean stableCalmTargetSurface(ServerWorld world,int targetX,int targetZ,int surfaceY){int[] adjacent=new int[CARDINAL_SURFACE_OFFSETS.length];for(int i=0;i<CARDINAL_SURFACE_OFFSETS.length;i++){int[] o=CARDINAL_SURFACE_OFFSETS[i];adjacent[i]=world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,targetX+o[0],targetZ+o[1]);}return stableCalmSurfaceNeighborhood(surfaceY,adjacent);}
    static boolean stableCalmSurfaceNeighborhood(int surfaceY,int... adjacent){if(adjacent==null||adjacent.length!=CARDINAL_SURFACE_OFFSETS.length)throw new IllegalArgumentException("CALM surface neighborhood requires four cardinal heights");for(int y:adjacent)if(Math.abs((long)y-surfaceY)>MAX_CALM_NEIGHBOR_SURFACE_DELTA)return false;return true;}
    static boolean stableCalmSurfaceProfile(int... ys){if(ys==null||ys.length==0)throw new IllegalArgumentException("CALM surface profile requires at least one height");for(int i=1;i<ys.length;i++)if(Math.abs((long)ys[i]-ys[i-1])>MAX_CALM_NEIGHBOR_SURFACE_DELTA)return false;return true;}
    private static boolean navigationPathSurfaceContinuous(ServerWorld world,Path path){if(world==null||path==null||path.getLength()==0)return false;int[] p=new int[path.getLength()];for(int i=0;i<path.getLength();i++){BlockPos n=path.getNode(i).getBlockPos();int y=world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,n.getX(),n.getZ());if(!stableCalmTargetSurface(world,n.getX(),n.getZ(),y))return false;p[i]=y;}return stableCalmSurfaceProfile(p);}
    private static boolean navigationPathPresentationClear(ServerWorld world,PokemonEntity actor,Path path){if(world==null||actor==null||path==null||path.getLength()==0)return false;boolean[] clear=new boolean[path.getLength()];for(int i=0;i<path.getLength();i++){BlockPos n=path.getNode(i).getBlockPos();var box=actor.getBoundingBox().offset(n.getX()+0.5D-actor.getX(),n.getY()-actor.getY(),n.getZ()+0.5D-actor.getZ());boolean blocks=world.isSpaceEmpty(actor,box);boolean overlap=!world.getOtherEntities(actor,box,c->c instanceof PokemonEntity&&VisibleWildPokemonEncounterRuntime.isInteractionActive(c.getUuid())).isEmpty();clear[i]=MareaWildCalmNavigationContinuityRuntime.presentationNodeClear(blocks,overlap);}return navigationPresentationProfileClear(clear);}
    static boolean navigationPresentationProfileClear(boolean... clear){if(clear==null||clear.length==0)return false;for(boolean c:clear)if(!c)return false;return true;}
    static int[] navigationTargetYCandidates(int actorY,int surfaceY){return actorY==surfaceY?new int[]{actorY}:new int[]{actorY,surfaceY};}
    static boolean navigationTargetInsideLeash(double cx,double cz,int r,double x,double z){if(!Double.isFinite(cx)||!Double.isFinite(cz)||!Double.isFinite(x)||!Double.isFinite(z)||r<=0)throw new IllegalArgumentException("native navigation target requires finite coordinates and positive leash");double dx=x-cx,dz=z-cz;return dx*dx+dz*dz<=(double)r*r;}
    static boolean navigationPathInsideLeash(double cx,double cz,int r,Path path){if(!Double.isFinite(cx)||!Double.isFinite(cz)||r<=0)throw new IllegalArgumentException("native navigation path requires finite center and positive leash");if(path==null||path.getLength()==0)return false;for(int i=0;i<path.getLength();i++){BlockPos n=path.getNode(i).getBlockPos();if(!navigationTargetInsideLeash(cx,cz,r,n.getX()+0.5D,n.getZ()+0.5D))return false;}return true;}
    private static double[] firstCollisionFreeVelocity(ServerWorld world,PokemonEntity actor,double cx,double cz,int r,double x,double z){boolean cw=clockwiseFirst(actor.getUuid());for(double a:TURN_ANGLES_DEGREES){double fa=cw?-a:a;double[] first=rotate(x,z,fa);if(candidateAllowed(world,actor,cx,cz,r,first))return first;double[] second=rotate(x,z,-fa);if(candidateAllowed(world,actor,cx,cz,r,second))return second;}return new double[]{0.0D,0.0D};}
    private static boolean candidateAllowed(ServerWorld world,PokemonEntity actor,double cx,double cz,int r,double[] v){return MareaWildAmbientBehaviorRuntime.insideLeashAfterImpulse(actor,cx,cz,r,v[0],v[1])&&isPresentationProbeClear(world,actor,v[0],v[1]);}
    private static boolean isPresentationProbeClear(ServerWorld world,PokemonEntity actor,double vx,double vz){double speed=Math.sqrt(vx*vx+vz*vz);if(speed<=MIN_HORIZONTAL_SPEED)return true;double scale=COLLISION_PROBE_DISTANCE/speed;var box=actor.getBoundingBox().offset(vx*scale,0.0D,vz*scale);boolean blocks=world.isSpaceEmpty(actor,box);boolean overlap=!world.getOtherEntities(actor,box,c->c instanceof PokemonEntity&&VisibleWildPokemonEncounterRuntime.isInteractionActive(c.getUuid())).isEmpty();return steeringProbePresentationClear(blocks,overlap);}
    static boolean steeringProbePresentationClear(boolean blockSpaceClear,boolean activeWildOverlap){return MareaWildCalmNavigationContinuityRuntime.presentationNodeClear(blockSpaceClear,activeWildOverlap);}
    static boolean clockwiseFirst(UUID actorId){if(actorId==null)throw new IllegalArgumentException("actorId is required");return((actorId.getMostSignificantBits()^actorId.getLeastSignificantBits())&1L)==0L;}
    static double[] rotate(double x,double z,double degrees){if(!Double.isFinite(x)||!Double.isFinite(z)||!Double.isFinite(degrees))throw new IllegalArgumentException("rotation requires finite velocity and angle");double rad=Math.toRadians(degrees),cos=Math.cos(rad),sin=Math.sin(rad);return new double[]{x*cos-z*sin,x*sin+z*cos};}
}
