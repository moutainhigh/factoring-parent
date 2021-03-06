package com.ph.shopping.facade.member.menum.promotion;

/**
 * 
 * factoring-facade-member
 *
 * @description：服务专员账号类型
 *
 * @author：liuy
 *
 * @createTime：2017年5月24日
 *
 * @Copyright @2017 by liuy
 */
public enum PromotionAccountTypeEnum {

	/**
	 * 新业态
	 */
    PROMOTION_XYT((byte) 1, "新业态"),
	/**
	 * 本地电商
	 */
    PROMOTION_LOCAL((byte) 2, "本地电商"),
	/**
	 * 平台添加
	 */
    PROMOTION_PLATFORM((byte) 3, "平台添加");
	
    /**
	 * 编码
	 */
	private Byte code;
	/**
	 * 描述
	 */
	private String remark;
	
	private PromotionAccountTypeEnum(Byte code,String remark){
		this.code = code;
		this.remark = remark;
	}

	public Byte getCode() {
		return code;
	}

	public void setCode(Byte code) {
		this.code = code;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
	
}
