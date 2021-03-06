package com.ph.shopping.facade.system.service;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ph.shopping.common.core.constant.PageConstant;
import com.ph.shopping.common.core.customenum.SystemOperateEnum;
import com.ph.shopping.common.util.container.ContainerUtil;
import com.ph.shopping.common.util.core.RespCode;
import com.ph.shopping.common.util.core.ResultUtil;
import com.ph.shopping.common.util.page.PageBean;
import com.ph.shopping.common.util.result.Result;
import com.ph.shopping.facade.mapper.SystemLogMapper;
import com.ph.shopping.facade.mapper.SystemParameterMapper;
import com.ph.shopping.facade.system.dto.SystemParameterDTO;
import com.ph.shopping.facade.system.dto.UpdateSystemParameterDTO;
import com.ph.shopping.facade.system.entity.SystemLog;
import com.ph.shopping.facade.system.entity.SystemParameter;
import com.ph.shopping.facade.system.exception.SystemBizException;
import com.ph.shopping.facade.system.exception.SystemEnum;
import com.ph.shopping.facade.system.senum.FixedParamEnum;
import com.ph.shopping.facade.system.vo.SystemParameterVO;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 参数设置实现类
 *
 * @author 郑朋
 * @create 2017/5/8
 **/
@Component
@Service(version = "1.0.0")
public class SystemParameterServiceImpl implements ISystemParameterService {
    private static final Logger logger = LoggerFactory.getLogger(SystemParameterServiceImpl.class);

    @Autowired
    SystemParameterMapper systemParameterMapper;

    @Autowired
    SystemLogMapper systemLogMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result updateSystemParameter(UpdateSystemParameterDTO updateSystemParameterDTO) {
        try {
            logger.info("修改系统参数入参，updateSystemParameterDTO={}", JSON.toJSONString(updateSystemParameterDTO));
            Result result = ResultUtil.getResult(RespCode.Code.REQUEST_DATA_ERROR);
            String errorStr = updateSystemParameterDTO.validateForm();
            if (StringUtils.isNotBlank(errorStr)) {
                result.setMessage(errorStr);
                return result;
            } else {
                //检验当前参数和是否小于
                Long temp[] = {101L,102L,103L,104L,105L,106L,107L,108L,109L,110L};
                if (ArrayUtils.contains(temp,updateSystemParameterDTO.getId())){
                    Double sumProfit = systemParameterMapper.getSumProfit(updateSystemParameterDTO.getId());
                    if ((sumProfit+updateSystemParameterDTO.getParameterValue())>=1){
                        logger.info("分润数值总和超过1");
                        result.setMessage("分润数值总和需小于1");
                        return result;
                    }
                }
                SystemParameter systemParameter = new SystemParameter();
                Map<String, Object> param = new HashedMap();
                param.put("parameter_value", updateSystemParameterDTO.getParameterValue());
                param.put("id", updateSystemParameterDTO.getId());
                param.put("remark",updateSystemParameterDTO.getRemark());
                //修改参数
                systemParameterMapper.updateSystemParameterById(param);
                // 修改关联的参数值
//                updateFixedValue(systemParameter.getId(), updateSystemParameterDTO.getOperatorId());
                SystemLog systemLog = new SystemLog();
                systemLog.setCreateTime(new Date());
                systemLog.setOperateAccount(updateSystemParameterDTO.getOperateAccount());
                systemLog.setCreaterId(updateSystemParameterDTO.getOperatorId());
                systemLog.setCreaterName(updateSystemParameterDTO.getOperatorName());
                systemLog.setOperateType(SystemOperateEnum.UPDATE.getType());
                systemLog.setOperateContent("修改参数配置，参数配置id" + updateSystemParameterDTO.getId()
                        + ", 修改参数值" + updateSystemParameterDTO.getParameterValue());
                //系统日志记录
                systemLogMapper.insert(systemLog);
                result = ResultUtil.getResult(SystemEnum.Code.SUCCESS);
            }
            logger.info("修改系统参数返回值，result={}", JSON.toJSONString(result));
            return result;
        } catch (Exception e) {
            logger.error("修改系统参数异常，e={}", e);
            throw new SystemBizException(SystemEnum.Code.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @throws
     * @Title: updatemFixedValue
     * @Description: 修改参数设置时联动修改固定值
     * @param: @param id
     * @return: void
     * @author：李杰
     */
    private void updateFixedValue(Long id, Long operatorId) {
        List<FixedParamEnum> fps = getFixedParam();
        if (null != fps && !fps.isEmpty()) {
            Long fixedId = null;
            for (FixedParamEnum fp : fps) {
                if (fp.getChangeId().equals(id)) {
                    fixedId = fp.getFixedId();
                    break;
                }
            }
            if (null != fixedId) {
                BigDecimal bd = new BigDecimal("0");
                for (FixedParamEnum fp : fps) {
                    if (fixedId.equals(fp.getFixedId())) {
                        bd = bd.add(new BigDecimal(Double.toString(fp.getChangeValue())));
                    }
                }
                SystemParameter systemParameter = new SystemParameter();
                systemParameter.setUpdaterId(operatorId);
                systemParameter.setUpdateTime(new Date());
                systemParameter.setId(fixedId);
                systemParameter.setParameterValue(bd.doubleValue());
                // 修改参数
                systemParameterMapper.updateByPrimaryKeySelective(systemParameter);
            }
        }
    }

    /**
     * @throws
     * @Title: getFixedParam
     * @Description: 得到固定的参数值
     * @param: @return
     * @return: Map<Long   ,   Double>
     * @author：李杰
     */
    private List<FixedParamEnum> getFixedParam() {
        List<FixedParamEnum> fps = ContainerUtil.lList();
        List<Long> ids = FixedParamEnum.getChangeIds();
        List<SystemParameterVO> fixedParams = systemParameterMapper.selectSystemParameterByIds(ids);
        if (null != fixedParams && !fixedParams.isEmpty()) {
            for (SystemParameterVO vo : fixedParams) {
                FixedParamEnum fp = FixedParamEnum.getFixedParamEnumByChangeId(vo.getId());
                if (null != fp) {
                    fp.setChangeValue(vo.getParameterValue());
                    fps.add(fp);
                }
            }
        }
        return fps;
    }

    /**
     * @param pageBean
     * @author: 张霞
     * @description：查询所有的系统参数
     * @createDate: 10:11 2017/6/15
     * @return: com.ph.shopping.common.util.result.Result
     * @version: 2.1
     */
    @Override
    public Result getSystemParameterList(PageBean pageBean) {
        if (pageBean != null) {
            pageBean.setPageSize(pageBean.getPageSize() == 0 ? PageConstant.PAGE_SIZE : pageBean.getPageSize());
            pageBean.setPageNum(pageBean.getPageNum() == 0 ? PageConstant.PAGE_NUM : pageBean.getPageNum());
        }
        try {
            PageHelper.startPage(pageBean.getPageNum(), pageBean.getPageSize());
            List<SystemParameterVO> list = systemParameterMapper.selectSystemParameterList();
            PageInfo<SystemParameterVO> pageInfo = new PageInfo<>(list);
            Result result = ResultUtil.getResult(SystemEnum.Code.SUCCESS, pageInfo.getList(), pageInfo.getTotal());
            logger.info("查询所有的系统参数，result={}", JSON.toJSONString(result));
            return result;
        } catch (Exception e) {
            logger.error("查询所有的系统参数异常，e={}", e);
            throw new SystemBizException(SystemEnum.Code.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Result getSystemParameterBySelective(SystemParameterDTO systemParameterDTO) {
        try {
            logger.info("条件查询系统参数入参，systemParameterDTO={}", JSON.toJSONString(systemParameterDTO));
            SystemParameter systemParameter = new SystemParameter();
            BeanUtils.copyProperties(systemParameterDTO, systemParameter);
            Result result = ResultUtil.getResult(SystemEnum.Code.SUCCESS, systemParameterMapper.selectSystemParameterBySelective(systemParameter));
            logger.info("条件查询系统参数返回值，result={}", JSON.toJSONString(result));
            return result;
        } catch (Exception e) {
            logger.error("条件查询系统参数异常，e={}", e);
            throw new SystemBizException(SystemEnum.Code.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Result getSystemParameterListByIds(List<Long> ids) {
        try {
            logger.info("通过id查询系统参数入参，systemParameterDTO={}", JSON.toJSONString(ids));
            Result result = ResultUtil.getResult(RespCode.Code.REQUEST_DATA_ERROR);
            if (CollectionUtils.isNotEmpty(ids)) {
                List<SystemParameterVO> list = systemParameterMapper.selectSystemParameterByIds(ids);
                Map<Long, Object> map = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(list)) {
                    for (SystemParameterVO vo : list) {
                        map.put(vo.getId(), vo.getParameterValue());
                    }
                }
                result = ResultUtil.getResult(SystemEnum.Code.SUCCESS, map);
            }
            logger.info("通过id查询系统参数返回值，result={}", JSON.toJSONString(result));
            return result;
        } catch (Exception e) {
            logger.error("通过id查询系统参数异常，e={}", e);
            throw new SystemBizException(SystemEnum.Code.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Double getSystemParameterById(Long id) {
        try {
            logger.info("通过id查询系统参数入参，systemParameterDTO={}", JSON.toJSONString(id));
            if (!ObjectUtils.isEmpty(id)) {
                SystemParameterVO systemParameterById = systemParameterMapper.getSystemParameterById(id);
                Map<Long, Object> map = new HashMap<>(16);
                if (systemParameterById != null) {
                    return systemParameterById.getParameterValue();
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("通过id查询系统参数异常，e={}", e);
            throw new SystemBizException(SystemEnum.Code.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @param type
     * @author: 荐家跃
     * @description：修改状态
     * @createDate: 9:59 2017/6/15
     * @return: com.ph.shopping.common.util.result.Result
     * @version: 2.1
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result updateType(UpdateSystemParameterDTO dto, Integer type) {
        Result result = new Result();
        try {
            logger.info("通过id查询系统参数入参，systemParameterDTO={}", JSON.toJSONString(type));
            systemParameterMapper.updateType(type);
            SystemLog systemLog = new SystemLog();
            systemLog.setCreateTime(new Date());
            systemLog.setOperateAccount(dto.getOperateAccount());
            systemLog.setCreaterId(dto.getOperatorId());
            systemLog.setCreaterName(dto.getOperatorName());
            systemLog.setOperateType(SystemOperateEnum.UPDATE.getType());
            systemLog.setOperateContent("参数设置：积分下的参数比例状态修改");
            //系统日志记录
            systemLogMapper.insert(systemLog);
            result.setMessage("操作成功");
            result.setCode("1");
            result.setSuccess(true);
        } catch (Exception e) {
            logger.error("通过id查询系统参数异常，e={}", e);
            throw new SystemBizException(SystemEnum.Code.INTERNAL_SERVER_ERROR);
        }
        return result;
    }

    /**
     * @param state
     * @author: 荐家跃
     * @description：修改积分启用状态
     * @createDate: 9:59 2017/6/15
     * @return: com.ph.shopping.common.util.result.Result
     * @version: 2.1
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result updateIntegratedState(UpdateSystemParameterDTO dto, Long state) {
        Result result = new Result();
        try {
            logger.info("通过id查询系统参数入参，systemParameterDTO={}", JSON.toJSONString(state));
            systemParameterMapper.updateIntegratedState(state);
            SystemLog systemLog = new SystemLog();
            systemLog.setCreateTime(new Date());
            systemLog.setOperateAccount(dto.getOperateAccount());
            systemLog.setCreaterId(dto.getOperatorId());
            systemLog.setCreaterName(dto.getOperatorName());
            systemLog.setOperateType(SystemOperateEnum.UPDATE.getType());
            systemLog.setOperateContent("参数设置：是否启用积分比例状态修改");
            //系统日志记录
            systemLogMapper.insert(systemLog);
            result.setMessage("操作成功");
            result.setCode("1");
            result.setSuccess(true);
        } catch (Exception e) {
            logger.error("通过id查询系统参数异常，e={}", e);
            throw new SystemBizException(SystemEnum.Code.INTERNAL_SERVER_ERROR);
        }
        return result;
    }
    /**
     * @param dto
     * @author: 荐家跃
     * @description：
     * @createDate: 10:14 2017/6/15
     * @return: com.ph.shopping.common.util.result.Result
     * @version: 2.1
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result newUpdateSystemParameter(UpdateSystemParameterDTO dto) {
        try {
            logger.info("修改系统参数入参，updateSystemParameterDTO={}", JSON.toJSONString(dto));
            Result result = ResultUtil.getResult(RespCode.Code.REQUEST_DATA_ERROR);
            String errorStr = dto.validateForm();
            if (StringUtils.isNotBlank(errorStr)) {
                result.setMessage(errorStr);
                return result;
            } else {
                SystemParameter systemParameter = new SystemParameter();
                Map<String, Object> param = new HashedMap();
                param.put("upperLimit", dto.getUpperLimit());
                param.put("lowerLimit",dto.getLowerLimit());
                param.put("id", dto.getId());
                param.put("remark",dto.getRemark());
                //修改参数
                systemParameterMapper.newUpdateSystemParameterById(param);
                // 修改关联的参数值
                SystemLog systemLog = new SystemLog();
                systemLog.setCreateTime(new Date());
                systemLog.setOperateAccount(dto.getOperateAccount());
                systemLog.setCreaterId(dto.getOperatorId());
                systemLog.setCreaterName(dto.getOperatorName());
                systemLog.setOperateType(SystemOperateEnum.UPDATE.getType());
                systemLog.setOperateContent("修改参数配置，参数配置id" + dto.getId()
                        + ", 修改参数值" + dto.getParameterValue());
                //系统日志记录
                systemLogMapper.insert(systemLog);
                result = ResultUtil.getResult(SystemEnum.Code.SUCCESS);
            }
            logger.info("修改系统参数返回值，result={}", JSON.toJSONString(result));
            return result;
        } catch (Exception e) {
            logger.error("修改系统参数异常，e={}", e);
            throw new SystemBizException(SystemEnum.Code.INTERNAL_SERVER_ERROR);
        }
    }

}
