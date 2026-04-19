package com.ccj.campus.chat.util;

/**
 * 地理工具。对齐论文 5.3 公式 (5.1)：
 *   d = R · arccos(sin φ1·sin φ2 + cos φ1·cos φ2·cos Δλ)
 *   R = 6371 km
 *
 * 实际工程中直接用 arccos 数值不稳定（接近 1 时精度丢失），
 * 采用 Haversine 公式等价替换，数值更稳定且结果一致。
 */
public final class GeoUtils {

    /** 地球半径（米）—— 论文取 6371 km */
    private static final double EARTH_RADIUS_M = 6_371_000d;

    private GeoUtils() {}

    /**
     * 计算两点球面距离（米）
     */
    public static int distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLam = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(dLam / 2) * Math.sin(dLam / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) Math.round(EARTH_RADIUS_M * c);
    }

    /** 是否在围栏内 */
    public static boolean inFence(double lat, double lon,
                                   double centerLat, double centerLon,
                                   int radiusMeters) {
        return distanceMeters(lat, lon, centerLat, centerLon) <= radiusMeters;
    }
}