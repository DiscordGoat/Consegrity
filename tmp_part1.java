        double micro = (fbm2(seed ^ 0x33AA55CCL, (double) wx * 0.06, (double) wz * 0.06) - 0.5) * 2.0 * 2.6;

        double baseH = base + plains + between * betweenAmp + micro + (ridge - 0.5) * 2.0 * 20.0 * mask + field;
        baseH += fbm2(seed ^ 0xE01234L, (double) wx * 0.004, (double) wz * 0.004) * 70.0 * mask;

        int yCap = Math.min(400, Math.max(256, world.getMaxHeight() - 1));
        double mmW = massiveMask(seed, wx, wz);
        if (mmW > 0.0) {
            V2 c = massiveMountainCenter(seed);
            double dx = (double) wx - c.x, dz = (double) wz - c.z;
            double d = Math.hypot(dx, dz);

            double rise = Math.pow(clamp01(1.0 - d / (MM_EFFECT_R)), 1.45);
            double H = MM_GROUND_Y + rise * (yCap - MM_GROUND_Y);
            if (d <= MM_CORE_R) {
                H = Math.max(H, yCap - 6);
            }
            if (d > MM_CROWN_IN && d < MM_CROWN_OUT) {
                double t = 1.0 - Math.abs((d - (MM_CROWN_IN + MM_CROWN_OUT) * 0.5) / ((MM_CROWN_OUT - MM_CROWN_IN) * 0.5));
                double spike = (valueNoise2(seed ^ 0x51ABCDL, (double) wx / 6.0, (double) wz / 6.0) - 0.5) * 2.0;
                H += Math.max(0.0, t) * (10.0 + spike * 14.0);
            }
            double step = 5.5;
            H = Math.floor(H / step) * step + (H - Math.floor(H / step) * step) * 0.35;
            if (d > MM_VALLEY_R0 && d < MM_VALLEY_R1) {
                double vt = 1.0 - Math.abs((d - (MM_VALLEY_R0 + MM_VALLEY_R1) * 0.5) / ((MM_VALLEY_R1 - MM_VALLEY_R0) * 0.5));
                H = lerp(H, MM_GROUND_Y, smooth01(clamp01(vt)) * 0.85);
            }
            double ring = clamp01(1.0 - Math.abs(d - (MM_VALLEY_R1 + MM_EFFECT_R) * 0.5) / (MM_EFFECT_R * 0.5));
            if (ring > 0.0) {
                double rr = (fbm2(seed ^ 0xA55A5AL, (double) wx / MM_RANGE_CELL, (double) wz / MM_RANGE_CELL) - 0.5) * 2.0;
                H += ring * rr * 60.0;
            }
            {
                double e1 = fbm2(seed ^ 0xE0B0DE5L, (double) wx * 0.02, (double) wz * 0.02);
                double e2 = fbm2(seed ^ 0xE0B0DE6L, (double) wx * 0.005, (double) wz * 0.005);
                double er = clamp01(e1 * 0.65 + e2 * 0.35);
                double u = clamp01(d / Math.max(1.0, MM_EFFECT_R));
                double wall = smooth01(1.0 - Math.abs(u - 0.5) / 0.5);
                double heightMask = smooth01(clamp01((H - MM_GROUND_Y) / Math.max(1.0, (yCap - MM_GROUND_Y))));
                double erosion = Math.pow(er, 1.2) * 18.0 * wall * heightMask;
                H = Math.max(MM_GROUND_Y, H - erosion);
            }
            baseH = Math.max(baseH, H);
        }
        if (baseH < 150.0) baseH = 150.0;
        if (baseH > yCap) baseH = yCap;
        return (int) Math.floor(baseH);
    }

