package com.jzo2o.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jzo2o.api.orders.OrdersApi;
import com.jzo2o.api.orders.OrdersServeApi;
import com.jzo2o.api.orders.dto.response.OrderResDTO;
import com.jzo2o.api.orders.dto.response.ServeProviderIdResDTO;
import com.jzo2o.common.expcetions.ForbiddenOperationException;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.customer.mapper.EvaluationRecordMapper;
import com.jzo2o.customer.model.domain.CommonUser;
import com.jzo2o.customer.model.domain.EvaluationRecord;
import com.jzo2o.customer.model.domain.ServeProvider;
import com.jzo2o.customer.model.dto.request.EvaluationVisibilityReqDTO;
import com.jzo2o.customer.model.dto.request.UnifiedEvaluationPageReqDTO;
import com.jzo2o.customer.model.dto.request.UnifiedEvaluationSubmitReqDTO;
import com.jzo2o.customer.model.dto.response.BooleanResDTO;
import com.jzo2o.customer.model.dto.response.EvaluationRateResDTO;
import com.jzo2o.customer.model.dto.response.UnifiedEvaluationResDTO;
import com.jzo2o.customer.model.enums.EvaluationTypeEnum;
import com.jzo2o.customer.model.enums.EvaluationVisibilityEnum;
import com.jzo2o.customer.service.ICommonUserService;
import com.jzo2o.customer.service.IServeProviderService;
import com.jzo2o.customer.service.UnifiedEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 项目内评价记录实现，所有端读取同一张 evaluation_record 表。 */
@Slf4j
@Service
public class UnifiedEvaluationServiceImpl implements UnifiedEvaluationService {
    private static final int WAITING_EVALUATION_STATUS = 400;

    @Resource
    private EvaluationRecordMapper evaluationRecordMapper;
    @Resource
    private OrdersApi ordersApi;
    @Resource
    private OrdersServeApi ordersServeApi;
    @Resource
    private ICommonUserService commonUserService;
    @Resource
    private IServeProviderService serveProviderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BooleanResDTO submit(Long consumerId, UnifiedEvaluationSubmitReqDTO req) {
        validateSubmit(req);
        OrderResDTO order = ordersApi.queryById(req.getOrdersId());
        if (order == null) {
            throw new ForbiddenOperationException("订单不存在");
        }
        if (!Objects.equals(order.getUserId(), consumerId)) {
            throw new ForbiddenOperationException("无权评价该订单");
        }
        if (!Objects.equals(order.getOrdersStatus(), WAITING_EVALUATION_STATUS)) {
            throw new ForbiddenOperationException("当前订单不能评价");
        }
        if (evaluationRecordMapper.selectCount(new LambdaQueryWrapper<EvaluationRecord>()
                .eq(EvaluationRecord::getOrdersId, req.getOrdersId())) > 0) {
            throw new ForbiddenOperationException("该订单已评价");
        }

        ServeProviderIdResDTO providerId = ordersServeApi.queryServeProviderIdByOrderId(req.getOrdersId());
        if (providerId == null || providerId.getServeProviderId() == null) {
            throw new ForbiddenOperationException("订单尚未分配服务人员，暂不能评价");
        }
        CommonUser consumer = commonUserService.getById(consumerId);
        ServeProvider worker = serveProviderService.findById(providerId.getServeProviderId());
        EvaluationRecord record = new EvaluationRecord();
        record.setOrdersId(order.getId());
        record.setConsumerId(consumerId);
        record.setServeProviderId(providerId.getServeProviderId());
        record.setEvaluationType(req.getEvaluationType());
        record.setVisibleStatus(EvaluationVisibilityEnum.VISIBLE.getCode());
        record.setContent(req.getContent().trim());
        record.setPictureArray(req.getPictureArray() == null ? null : JSONUtil.toJsonStr(req.getPictureArray()));
        record.setOrdersNo(StrUtil.blankToDefault(order.getOrdersCode(), String.valueOf(order.getId())));
        record.setServeItemName(order.getServeItemName());
        record.setServeItemImg(order.getServeItemImg());
        record.setServeAddress(order.getServeAddress());
        record.setServeStartTime(order.getServeStartTime());
        record.setConsumerName(consumer == null ? order.getContactsName() : consumer.getNickname());
        record.setConsumerPhone(EvaluationContactResolver.resolvePhone(
                consumer == null ? null : consumer.getPhone(), order.getContactsPhone()));
        record.setServeProviderName(worker == null ? order.getServerName() : worker.getName());
        record.setIsAnonymous(req.getIsAnonymous() == null ? 0 : req.getIsAnonymous());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        evaluationRecordMapper.insert(record);
        // 评价内容已成功落库时，订单状态机的短暂异常不能让客户误以为评价失败。
        // 旧评价流程也采用同样的容错策略；异常会保留在日志中供排查。
        try {
            ordersApi.evaluate(order.getId());
        } catch (Exception e) {
            log.error("评价已保存，但订单状态更新失败，订单id={}", order.getId(), e);
        }
        return new BooleanResDTO(true);
    }

    @Override
    public PageResult<UnifiedEvaluationResDTO> pageForConsumer(Long consumerId, UnifiedEvaluationPageReqDTO req) {
        LambdaQueryWrapper<EvaluationRecord> wrapper = baseVisibleWrapper(req)
                .eq(EvaluationRecord::getConsumerId, consumerId);
        return page(wrapper, req);
    }

    @Override
    public PageResult<UnifiedEvaluationResDTO> pageForWorker(Long workerId, UnifiedEvaluationPageReqDTO req) {
        LambdaQueryWrapper<EvaluationRecord> wrapper = baseVisibleWrapper(req)
                .eq(EvaluationRecord::getServeProviderId, workerId);
        return page(wrapper, req);
    }

    @Override
    public PageResult<UnifiedEvaluationResDTO> pageForOperation(UnifiedEvaluationPageReqDTO req) {
        LambdaQueryWrapper<EvaluationRecord> wrapper = new LambdaQueryWrapper<EvaluationRecord>()
                .eq(req.getEvaluationType() != null, EvaluationRecord::getEvaluationType, req.getEvaluationType())
                .eq(req.getVisibleStatus() != null, EvaluationRecord::getVisibleStatus, req.getVisibleStatus())
                .eq(req.getServeProviderId() != null, EvaluationRecord::getServeProviderId, req.getServeProviderId())
                .like(StrUtil.isNotBlank(req.getOrdersNo()), EvaluationRecord::getOrdersNo, req.getOrdersNo())
                .like(StrUtil.isNotBlank(req.getConsumerPhone()), EvaluationRecord::getConsumerPhone, req.getConsumerPhone());
        return page(wrapper, req);
    }

    @Override
    public UnifiedEvaluationResDTO detailForConsumer(Long id, Long consumerId) {
        return toRes(required(id, EvaluationRecord::getConsumerId, consumerId, true));
    }

    @Override
    public UnifiedEvaluationResDTO detailForWorker(Long id, Long workerId) {
        return toRes(required(id, EvaluationRecord::getServeProviderId, workerId, true));
    }

    @Override
    public UnifiedEvaluationResDTO detailForOperation(Long id) {
        EvaluationRecord record = evaluationRecordMapper.selectById(id);
        if (record == null) {
            throw new ForbiddenOperationException("评价不存在");
        }
        return toRes(record);
    }

    @Override
    public EvaluationRateResDTO rateForWorker(Long workerId) {
        long good = evaluationRecordMapper.selectCount(new LambdaQueryWrapper<EvaluationRecord>()
                .eq(EvaluationRecord::getServeProviderId, workerId)
                .eq(EvaluationRecord::getVisibleStatus, EvaluationVisibilityEnum.VISIBLE.getCode())
                .eq(EvaluationRecord::getEvaluationType, EvaluationTypeEnum.GOOD.getCode()));
        long bad = evaluationRecordMapper.selectCount(new LambdaQueryWrapper<EvaluationRecord>()
                .eq(EvaluationRecord::getServeProviderId, workerId)
                .eq(EvaluationRecord::getVisibleStatus, EvaluationVisibilityEnum.VISIBLE.getCode())
                .eq(EvaluationRecord::getEvaluationType, EvaluationTypeEnum.BAD.getCode()));
        return EvaluationRateResDTO.of(good, bad);
    }

    @Override
    public Map<Long, EvaluationRateResDTO> rateForWorkers(List<Long> workerIds) {
        if (workerIds == null || workerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> distinctWorkerIds = new ArrayList<>(new LinkedHashSet<>(workerIds));
        distinctWorkerIds.removeIf(Objects::isNull);
        if (distinctWorkerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<EvaluationRecord> records = evaluationRecordMapper.selectList(
                new LambdaQueryWrapper<EvaluationRecord>()
                        .in(EvaluationRecord::getServeProviderId, distinctWorkerIds)
                        .eq(EvaluationRecord::getVisibleStatus, EvaluationVisibilityEnum.VISIBLE.getCode()));
        return EvaluationRateBatchCalculator.calculate(distinctWorkerIds, records);
    }

    @Override
    public void updateVisibility(Long id, EvaluationVisibilityReqDTO req) {
        if (req == null || !EvaluationVisibilityEnum.isValid(req.getVisibleStatus())) {
            throw new ForbiddenOperationException("展示状态不正确");
        }
        EvaluationRecord record = evaluationRecordMapper.selectById(id);
        if (record == null) {
            throw new ForbiddenOperationException("评价不存在");
        }
        record.setVisibleStatus(req.getVisibleStatus());
        record.setUpdateTime(LocalDateTime.now());
        evaluationRecordMapper.updateById(record);
    }

    private void validateSubmit(UnifiedEvaluationSubmitReqDTO req) {
        if (req == null || req.getOrdersId() == null) {
            throw new ForbiddenOperationException("订单不能为空");
        }
        if (!EvaluationTypeEnum.isValid(req.getEvaluationType())) {
            throw new ForbiddenOperationException("请选择好评或差评");
        }
        if (StrUtil.isBlank(req.getContent())) {
            throw new ForbiddenOperationException("请填写评价内容");
        }
        if (req.getPictureArray() != null && req.getPictureArray().length > 9) {
            throw new ForbiddenOperationException("最多上传9张评价图片");
        }
    }

    private LambdaQueryWrapper<EvaluationRecord> baseVisibleWrapper(UnifiedEvaluationPageReqDTO req) {
        return new LambdaQueryWrapper<EvaluationRecord>()
                .eq(EvaluationRecord::getVisibleStatus, EvaluationVisibilityEnum.VISIBLE.getCode())
                .eq(req.getEvaluationType() != null, EvaluationRecord::getEvaluationType, req.getEvaluationType());
    }

    private PageResult<UnifiedEvaluationResDTO> page(LambdaQueryWrapper<EvaluationRecord> wrapper,
                                                       UnifiedEvaluationPageReqDTO req) {
        long pageNo = req.getPageNo() == null || req.getPageNo() < 1 ? 1 : req.getPageNo();
        long pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 10 : Math.min(req.getPageSize(), 100);
        Page<EvaluationRecord> page = evaluationRecordMapper.selectPage(new Page<>(pageNo, pageSize),
                wrapper.orderByDesc(EvaluationRecord::getCreateTime));
        List<UnifiedEvaluationResDTO> list = new ArrayList<>();
        for (EvaluationRecord record : page.getRecords()) {
            list.add(toRes(record));
        }
        return new PageResult<>(page.getPages(), page.getTotal(), list);
    }

    private EvaluationRecord required(Long id, java.util.function.Function<EvaluationRecord, Long> ownerGetter,
                                      Long ownerId, boolean visibleOnly) {
        EvaluationRecord record = evaluationRecordMapper.selectById(id);
        if (record == null || !Objects.equals(ownerGetter.apply(record), ownerId)
                || (visibleOnly && !Objects.equals(record.getVisibleStatus(), EvaluationVisibilityEnum.VISIBLE.getCode()))) {
            throw new ForbiddenOperationException("评价不存在或无权查看");
        }
        return record;
    }

    private UnifiedEvaluationResDTO toRes(EvaluationRecord record) {
        fillMissingConsumerPhone(record);
        UnifiedEvaluationResDTO res = BeanUtil.toBean(record, UnifiedEvaluationResDTO.class);
        if (StrUtil.isNotBlank(record.getPictureArray())) {
            List<String> pictures = JSONUtil.parseArray(record.getPictureArray()).toList(String.class);
            res.setPictureArray(pictures.toArray(new String[0]));
        } else {
            res.setPictureArray(new String[0]);
        }
        return res;
    }

    /**
     * 兼容首次改版前已生成、但没有写入联系电话的评价快照。
     * 只在电话号码为空时回查订单，并将结果写回，避免后续列表重复远程查询。
     */
    private void fillMissingConsumerPhone(EvaluationRecord record) {
        if (StrUtil.isNotBlank(record.getConsumerPhone())) {
            return;
        }
        try {
            OrderResDTO order = ordersApi.queryById(record.getOrdersId());
            String contactsPhone = order == null ? null : order.getContactsPhone();
            if (StrUtil.isNotBlank(contactsPhone)) {
                record.setConsumerPhone(contactsPhone);
                record.setUpdateTime(LocalDateTime.now());
                evaluationRecordMapper.updateById(record);
            }
        } catch (Exception e) {
            log.debug("评价记录联系电话回填失败，评价id={}", record.getId(), e);
        }
    }
}
