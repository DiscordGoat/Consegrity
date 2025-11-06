package goat.projectLinearity.libs.mutation;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility helpers for creating custom player heads from texture payloads.
 */
final class MutationHeadUtil {

    private MutationHeadUtil() {
    }

    static ItemStack createHead(String base64Texture) {
        if (base64Texture == null || base64Texture.isEmpty()) {
            return null;
        }

        try {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta == null) {
                return null;
            }

            byte[] decoded = Base64.getDecoder().decode(base64Texture);
            String json = new String(decoded, StandardCharsets.UTF_8);
            int urlStart = json.indexOf("http");
            if (urlStart < 0) {
                return null;
            }
            int urlEnd = json.indexOf('"', urlStart);
            if (urlEnd < 0) {
                return null;
            }
            String urlString = json.substring(urlStart, urlEnd);

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(urlString), PlayerTextures.SkinModel.CLASSIC);
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            skull.setItemMeta(meta);
            return skull;
        } catch (Exception ignored) {
            return null;
        }
    }
}
