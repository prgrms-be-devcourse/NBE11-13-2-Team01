package com.example.delivery_project.service.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskFactorCalculator {
    //시간 당 강수량 30mm 이상이면 HEAVY_RAIN
    //rn1 : "강수없음" / "1mm 이상" / "30mm 이상 50mm 미만" 등의 문자열로 저장
    public boolean isHeavyRain(String rn1,String pty){
        if(!isRain(pty)){
            return false;
        }
        if(rn1==null || rn1.equals("강수없음")||rn1.contains("미만")){
            return false;
        }
        if(rn1.contains("이상")||rn1.contains("~")){
            return true;
        }

        double mm = Double.parseDouble(rn1.replace("mm","").trim());
        return mm>=30.0;
    }
    //기온 33 이상이면 HEAT_WAVE
    public boolean isHeatWave(String t1h){
        if(t1h==null){
            return false;
        }
        double temperature = Double.parseDouble(t1h);
        return temperature>=33;
    }
    //pty의 값이 1, 4, 5면 비
    public boolean isRain(String pty){
        int code = Integer.parseInt(pty);
        return code == 1 || code == 4 || code ==5;
    }
}
