package thunder.hack.core;

import com.google.common.eventbus.Subscribe;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import thunder.hack.Thunderhack;
import thunder.hack.events.impl.EventPostTick;
import thunder.hack.events.impl.EventTick;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.TotemPopEvent;
import thunder.hack.modules.Module;
import thunder.hack.modules.combat.AntiBot;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static thunder.hack.modules.Module.mc;

public class CombatManager {
    public HashMap<String, Integer> popList = new HashMap<>();

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (Module.fullNullCheck()) return;

        if (event.getPacket() instanceof EntityStatusS2CPacket pac) {
            if (pac.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING) {
                Entity ent = pac.getEntity(mc.world);
                if(!(ent instanceof PlayerEntity)) return;
                if (popList == null) {
                    popList = new HashMap<>();
                }
                if (popList.get(ent.getName().getString()) == null) {
                    popList.put(ent.getName().getString(), 1);
                } else if (popList.get(ent.getName().getString()) != null) {
                    popList.put(ent.getName().getString(),  popList.get(ent.getName().getString()) + 1);
                }
                Thunderhack.EVENT_BUS.post(new TotemPopEvent((PlayerEntity) ent, popList.get(ent.getName().getString())));
            }
        }
    }

    @EventHandler
    public void onPostTick(EventPostTick event) {
        if (Module.fullNullCheck()) {
            return;
        }
        for (PlayerEntity player : mc.world.getPlayers()) {
            if(AntiBot.bots.contains(player)) return;
            if (player.getHealth() <= 0 && popList.containsKey(player.getName().getString())) {
                popList.remove(player.getName().getString(), popList.get(player.getName().getString()));
            }
        }
    }

    public int getPops(PlayerEntity entity){
        if(popList.get(entity.getName().getString()) == null) return 0;
        return popList.get(entity.getName().getString());
    }

    public List<PlayerEntity> getTargets(float range) {
        return mc.world.getPlayers().stream()
                .filter(e -> !e.isDead())
                .filter(entityPlayer -> !Thunderhack.friendManager.isFriend(entityPlayer.getName().getString()))
                .filter(entityPlayer -> entityPlayer != mc.player)
                .filter(entityPlayer -> mc.player.distanceTo(entityPlayer) < range)
                .sorted(Comparator.comparing(e -> mc.player.distanceTo(e)))
                .collect(Collectors.toList());
    }

    public PlayerEntity getNearestTarget(float range){
        return mc.world.getPlayers()
                .stream()
                .filter(e -> e != mc.player)
                .filter(e -> !e.isDead())
                .filter(e -> !Thunderhack.friendManager.isFriend(e.getName().getString()))
                .filter(e -> e.getHealth() > 0)
                .filter(entityPlayer -> mc.player.distanceTo(entityPlayer) < range)
                .min(Comparator.comparing(t -> mc.player.distanceTo(t))).orElse(null);
    }

    public PlayerEntity getTargetByHP(float range){
        return mc.world.getPlayers()
                .stream()
                .filter(e -> e != mc.player)
                .filter(e -> !e.isDead())
                .filter(e -> !Thunderhack.friendManager.isFriend(e.getName().getString()))
                .filter(e -> e.getHealth() > 0)
                .filter(entityPlayer -> mc.player.distanceTo(entityPlayer) < range)
                .min(Comparator.comparing(t -> (t.getHealth() + t.getAbsorptionAmount()))).orElse(null);
    }

}
