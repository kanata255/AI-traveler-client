package com.example.demo.plan;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.common.BizException;
import com.example.demo.enticy.PlanTrip;
import com.example.demo.mapper.PlanTripMapper;
import com.example.demo.plan.dto.SavePlanTripRequest;
import com.example.demo.vo.PlanTripVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class PlanTripService extends ServiceImpl<PlanTripMapper, PlanTrip> {

    @Transactional
    public Long save(SavePlanTripRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> trip = req.trip();
        if (trip == null || trip.isEmpty()) {
            throw new BizException(400, "trip 不能为空");
        }

        String planTaskId = req.planTaskId();
        PlanTrip existing = getOne(new LambdaQueryWrapper<PlanTrip>()
                .eq(PlanTrip::getUserId, userId)
                .eq(PlanTrip::getPlanTaskId, planTaskId), false);

        String tripJson = JSONUtil.toJsonStr(trip);
        if (existing != null) {
            existing.setTripJson(tripJson);
            existing.setTitle(str(trip.get("title")));
            existing.setDestination(str(trip.get("destination")));
            existing.setStartDate(str(trip.get("startDate")));
            existing.setEndDate(str(trip.get("endDate")));
            updateById(existing);
            return existing.getId();
        }

        PlanTrip entity = new PlanTrip();
        entity.setUserId(userId);
        entity.setPlanTaskId(planTaskId);
        entity.setTripJson(tripJson);
        entity.setTitle(str(trip.get("title")));
        entity.setDestination(str(trip.get("destination")));
        entity.setStartDate(str(trip.get("startDate")));
        entity.setEndDate(str(trip.get("endDate")));
        save(entity);
        return entity.getId();
    }

    public List<PlanTripVO> listTrips() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<PlanTrip> trips = list(new LambdaQueryWrapper<PlanTrip>()
                .eq(PlanTrip::getUserId, userId)
                .orderByDesc(PlanTrip::getCreateTime));
        return trips.stream().map(this::toVo).toList();
    }

    private PlanTripVO toVo(PlanTrip e) {
        PlanTripVO vo = new PlanTripVO();
        vo.setId(e.getId());
        vo.setPlanTaskId(e.getPlanTaskId());
        vo.setTitle(e.getTitle());
        vo.setDestination(e.getDestination());
        vo.setStartDate(e.getStartDate());
        vo.setEndDate(e.getEndDate());
        vo.setTrip(JSONUtil.parseObj(e.getTripJson()));
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }
}