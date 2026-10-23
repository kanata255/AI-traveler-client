package com.example.demo.plan;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.demo.common.Result;
import com.example.demo.plan.dto.SavePlanTripRequest;
import com.example.demo.vo.PlanTripVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "行程计划保存", description = "保存 / 查询 AI 生成的旅行计划")
@RestController
@RequestMapping("/v1/plan/trips")
public class PlanTripController {

    private final PlanTripService planTripService;

    public PlanTripController(PlanTripService planTripService) {
        this.planTripService = planTripService;
    }

    @Operation(summary = "保存旅行计划", description = "按 planTaskId 幂等保存（同任务重复保存会覆盖）")
    @SaCheckLogin
    @PostMapping
    public Result<Long> save(@RequestBody @Valid SavePlanTripRequest req) {
        return Result.success(planTripService.save(req));
    }

    @Operation(summary = "查询当前用户全部旅行计划", description = "按创建时间倒序返回")
    @SaCheckLogin
    @GetMapping
    public Result<List<PlanTripVO>> list() {
        return Result.success(planTripService.listTrips());
    }
}