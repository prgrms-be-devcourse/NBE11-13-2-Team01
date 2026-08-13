package com.example.delivery_project.service.component;

public class LocationConverter {

    //mode
    public static final int TO_GRID = 0;
    public static final int TO_GPS = 1;

    private static final double RE = 6371.00877; // 지구 반경(km)
    private static final double GRID = 5.0;       // 격자 간격(km)
    private static final double SLAT1 = 30.0;     // 투영 위도1(degree)
    private static final double SLAT2 = 60.0;     // 투영 위도2(degree)
    private static final double OLON = 126.0;     // 기준점 경도(degree)
    private static final double OLAT = 38.0;      // 기준점 위도(degree)
    private static final double XO = 43;          // 기준점 X좌표(GRID)
    private static final double YO = 136;         // 기준점 Y좌표(GRID)

    private static final double DEGRAD = Math.PI / 180.0;
    private static final double RADDEG = 180.0 / Math.PI;

    private LocationConverter() {
    }

    // LCC DFS 좌표변환 ( mode: TO_GRID(위경도->좌표, latX:위도, lngY:경도), TO_GPS(좌표->위경도, latX:x, lngY:y) )

    public static LatXLngY convertGridGps(int mode, double latX, double lngY) {
        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        LatXLngY rs = new LatXLngY();

        if (mode == TO_GRID) {
            rs.lat = latX;
            rs.lng = lngY;

            double ra = Math.tan(Math.PI * 0.25 + latX * DEGRAD * 0.5);
            ra = re * sf / Math.pow(ra, sn);
            double theta = lngY * DEGRAD - olon;
            if (theta > Math.PI) {
                theta -= 2.0 * Math.PI;
            }
            if (theta < -Math.PI) {
                theta += 2.0 * Math.PI;
            }
            theta *= sn;

            rs.x = Math.floor(ra * Math.sin(theta) + XO + 0.5);
            rs.y = Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
        } else {
            rs.x = latX;
            rs.y = lngY;

            double xn = latX - XO;
            double yn = ro - lngY + YO;
            double ra = Math.sqrt(xn * xn + yn * yn);
            if (sn < 0.0) {
                ra = -ra;
            }
            double alat = Math.pow((re * sf / ra), (1.0 / sn));
            alat = 2.0 * Math.atan(alat) - Math.PI * 0.5;

            double theta;
            if (Math.abs(xn) <= 0.0) {
                theta = 0.0;
            } else if (Math.abs(yn) <= 0.0) {
                theta = Math.PI * 0.5;
                if (xn < 0.0) {
                    theta = -theta;
                }
            } else {
                theta = Math.atan2(xn, yn);
            }

            double alon = theta / sn + olon;
            rs.lat = alat * RADDEG;
            rs.lng = alon * RADDEG;
        }

        return rs;
    }

    public static class LatXLngY {
        public double lat;
        public double lng;
        public double x;
        public double y;
    }
}
