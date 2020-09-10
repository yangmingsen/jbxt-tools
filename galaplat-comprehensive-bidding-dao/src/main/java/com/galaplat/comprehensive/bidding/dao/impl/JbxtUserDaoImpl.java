package com.galaplat.comprehensive.bidding.dao.impl;

import java.util.List;

import com.galaplat.base.core.common.utils.BeanCopyUtils;
import com.galaplat.comprehensive.bidding.dao.dvos.SupplierAccountExportDVO;
import com.galaplat.comprehensive.bidding.dao.dvos.SupplierDVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.galaplat.base.core.common.exception.BaseException;
import com.galaplat.comprehensive.bidding.dao.IJbxtUserDao;
import com.galaplat.comprehensive.bidding.dao.dos.JbxtUserDO;
import com.galaplat.comprehensive.bidding.dao.dvos.JbxtUserDVO;
import com.galaplat.comprehensive.bidding.dao.mappers.custs.JbxtUserCustMapper;
import com.galaplat.comprehensive.bidding.dao.params.JbxtUserParam;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

/**
 * 用户表DaoImpl
 * @author esr
 * @date: 2020年06月17日
 */
@Repository
public  class JbxtUserDaoImpl implements IJbxtUserDao  {

	@Autowired
	private JbxtUserCustMapper mapper;

	@Override
	public List<JbxtUserDVO> findAllByActivityCode(String activityCode) {
		return mapper.findAllByActivityCode(activityCode);
	}

	@Override
	public int insertJbxtUser(JbxtUserDO entity){
		return mapper.insert(entity);
	}

	@Override
	public int updateJbxtUser(JbxtUserDO entity){
		return mapper.updateByPrimaryKey(entity);
	}

	@Override
	public PageInfo<JbxtUserDVO> getJbxtUserPage(JbxtUserParam jbxtuserParam) throws BaseException{
		PageHelper.startPage(jbxtuserParam.getPn(), jbxtuserParam.getPs());
		return new PageInfo<>(mapper.getJbxtUserList(jbxtuserParam));

	}

	@Override
	public JbxtUserDO getJbxtUser(JbxtUserParam jbxtuserParam){
		return mapper.selectByPrimaryKey(jbxtuserParam.getCode());
	}

	@Override
	public JbxtUserDO getJbxtUserByUsername(String username){
		return mapper.selectByUsernameKey(username);
	}

	@Override
	public JbxtUserDO selectByuserCodeAndActivityCode(String userCode, String activityCode) {
		return mapper.selectByuserCodeAndActivityCode(userCode,activityCode);
	}

	@Override
	public JbxtUserDO selectByUsernameAndActivityCode(String username, String activityCode) {
		return mapper.selectByUsernameAndActivityCode(username,activityCode);
	}

	@Override
	public int btachInsertAndUpdate(List<JbxtUserParam> userParams) {
		return mapper.btachInsertAndUpdate(userParams);
	}

	@Override
	public List<JbxtUserDO> getUser(JbxtUserParam userParam) {
		return mapper.getUser(userParam);
	}

	@Override
	public List<SupplierAccountExportDVO> getAccountByActivityCode(JbxtUserParam userParam) {
		return mapper.getAccountByActivityCode(userParam);
	}

	@Override
	public int deleteUser(JbxtUserParam userParam) {
		return mapper.deleteUser(userParam);
	}

	@Override
	public int batchDeleteUser(List<String> userCodes, String activityCode) {
		return mapper.batchDeleteUser(userCodes, activityCode);
	}

	@Override
	public  List<JbxtUserDO> listJbxtUser(JbxtUserParam param) {
       return  mapper.listJbxtUser(param);
	}

	@Override
	public int updateBySomeParam(JbxtUserParam updateParam,  JbxtUserParam conditionParam) {
		return  mapper.updateBySomeParam(updateParam,conditionParam);
	}

	@Override
	public List<SupplierDVO> listSupplierInfo(String bidActivityCode) {
		List<JbxtUserDO> userDOList = mapper.listJbxtUser(JbxtUserParam.builder().activityCode(bidActivityCode).build());
		return BeanCopyUtils.copyList(userDOList, SupplierDVO.class);
	}

	@Override
	public  JbxtUserDO getUserByParam( JbxtUserParam param) {
		return mapper.getJbxtUser(param);
	}

}
