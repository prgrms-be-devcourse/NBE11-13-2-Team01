package com.example.delivery_project.service.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class RiskFactorCalculator {
    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("\\d+(?:\\.\\d+)?");

    //시간 당 강수량 30mm 이상이면 HEAVY_RAIN
    //rn1 : "강수없음" / "1mm 이상" / "30mm 이상 50mm 미만" 등의 문자열로 저장
    public boolean isHeavyRain(String rn1, String pty) {
        if (!isRain(pty)) {
            return false;
        }
        if (rn1 == null || rn1.isBlank() || rn1.equals("강수없음")) {
            return false;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(rn1);
        if (!matcher.find()) {
            return false;
        }

        double minimumRainfall = Double.parseDouble(matcher.group());
        return minimumRainfall >= 30.0;
    }

    //기온 33 이상이면 HEAT_WAVE
    public boolean isHeatWave(String t1h) {
        if (t1h == null || t1h.isBlank()) {
            return false;
        }
        try {
            double temperature = Double.parseDouble(t1h);
            return temperature >= 33;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    //pty의 값이 1, 4, 5면 비
    public boolean isRain(String pty) {
        if (pty == null || pty.isBlank()) {
            return false;
        }
        try {
            int code = Integer.parseInt(pty);
            return code == 1 || code == 4 || code == 5;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
