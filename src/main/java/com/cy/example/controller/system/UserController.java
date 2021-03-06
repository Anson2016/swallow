package com.cy.example.controller.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.cy.example.carrier.PageCa;
import com.cy.example.config.WebConfig;
import com.cy.example.controller.BaseController;
import com.cy.example.entity.system.LoginRecordEntity;
import com.cy.example.entity.system.MailEntity;
import com.cy.example.entity.system.SysDepartmentEntity;
import com.cy.example.entity.system.SysUserEntity;
import com.cy.example.service.IDepartmentService;
import com.cy.example.service.IMailService;
import com.cy.example.service.IUserService;
import com.cy.example.supplement.rabbitmq.general.RabbitSender;
import com.cy.example.util.MD5Util;
import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/system/user")
public class UserController extends BaseController {

	@Autowired
	private IUserService userService;
	
	@Autowired
	private IMailService mailService;
	
	@Autowired
	private RabbitSender rabbitSender;

	private final Logger logger = LoggerFactory
			.getLogger(this.getClass());
	
	@RequestMapping("/register")
	public Map<String, Object> register(@ModelAttribute("user") SysUserEntity user) {
		WebConfig.add(user);
		user.setN_status("0");
		boolean flag = userService.insert(user);
		MailEntity mail = new MailEntity();
		//后面优化为给数据库中的管理员发送
		mail.setTo("pjchenyang@qq.com");
    	mail.setSubject("用户:"+user.getC_username()+"的注册审核提醒");
    	mail.setContent(user.toStringCN());
		rabbitSender.sendMail(mail);
		Map<String, Object> map = new HashMap<String, Object>();
		if (flag) {
			map.put("flag", flag);
			map.put("msg", "注册成功！请等待管理员激活！");
		} else {
			map.put("flag", flag);
			map.put("msg", "注册失败！");
		}
		return map;
	}
	
	@RequestMapping("/lock")
	public Map<String, Object> lock(long id,String n_status) {
		SysUserEntity user = userService.selectById(id);
		String msg = "";
		user.setN_status(n_status);
		WebConfig.update(user);
		boolean flag = userService.updateById(user);
		Map<String, Object> map = new HashMap<String, Object>();
		if (flag) {
			map.put("flag", flag);
			map.put("msg", msg+"成功！");
		} else {
			map.put("flag", flag);
			map.put("msg", msg+"失败！");
		}
		return map;
	}

	@PostMapping
	public Map<String, Object> add(@ModelAttribute("user") SysUserEntity user) {
		user.getN_departmentId().setId(Long.valueOf(user.getN_departmentId().getC_departName()));
		boolean flag = userService.insertMy(user);
		Map<String, Object> map = new HashMap<String, Object>();
		if (flag) {
			map.put("flag", flag);
			map.put("msg", "添加成功！");
		} else {
			map.put("flag", flag);
			map.put("msg", "添加失败！");
		}
		return map;
	}

	@PutMapping
	public Map<String, Object> update(@ModelAttribute("user") SysUserEntity user) {
		/*if("男".equals(user.getN_sex())){
			user.setN_sex("1");
		}else if("女".equals(user.getN_sex())){
			user.setN_sex("0");
		}*/
		/*if("可用".equals(user.getN_status())){
			user.setN_status("1");
		}else if("锁定".equals(user.getN_sex())){
			user.setN_status("0");
		}*/
		user.getN_departmentId().setId(Long.valueOf(user.getN_departmentId().getC_departName()));
		boolean flag = userService.updateMy(user);
		Map<String, Object> map = new HashMap<String, Object>();
		if (flag) {
			map.put("flag", true);
			map.put("msg", "更新成功！");
		} else {
			map.put("flag", false);
			map.put("msg", "更新失败！");
		}
		return map;
	}

	@DeleteMapping("/{id}")
	public Map<String, Object> delete(@PathVariable("id")Long id) {
		boolean flag = userService.deleteById(id);
		Map<String, Object> map = new HashMap<String, Object>();
		if (flag) {
			map.put("flag", flag);
			map.put("msg", "删除成功！");
		} else {
			map.put("flag", flag);
			map.put("msg", "删除失败！");
		}
		return map;
	}

	@GetMapping
	public Map<String, Object> findAll(@ModelAttribute("page") PageCa page)
			throws JsonProcessingException {
		// System.out.print("================================="+page.toString()+page.getIndex());
		List<SysUserEntity> list = userService.findAll(page);

		Map<String, Object> map = new HashMap<String, Object>();
		int sum = userService.selectCount(new EntityWrapper<SysUserEntity>());
		
		for(SysUserEntity entity : list){
			if("1".equals(entity.getN_sex())){
				entity.setN_sex("男");
			}else if("0".equals(entity.getN_sex())){
				entity.setN_sex("女");
			}
			if("1".equals(entity.getN_status())){
				entity.setN_status("可用");
			}else if("0".equals(entity.getN_status())){
				entity.setN_status("锁定");
			}
		}
		map.put("rows", list);
		map.put("total", sum);
		return map;
	}

	@GetMapping("/search")
	public Map<String, Object> search(
			@ModelAttribute("user") SysUserEntity user,
			@ModelAttribute("page") PageCa page) {
		Map<String, Object> map = new HashMap<String, Object>();
		List<SysUserEntity> users = userService.searchAll(user, page);
		int sum = userService.searchAllCount(user);
		map.put("rows", users);
		map.put("total", sum);
		return map;
	}

	@SuppressWarnings("finally")
	@RequestMapping("/validate")
	public Map<String, Object> validate(String username, String password,Boolean rememberMe) {
		Map<String, Object> map = new HashMap<String, Object>();
		password = MD5Util.GetMD5Code(password);
		UsernamePasswordToken token = new UsernamePasswordToken(username, password,rememberMe);
		boolean flag = true;
		String msg = "";
		try {
			Subject subject = SecurityUtils.getSubject();
			subject.login(token); // 完成登录
			SysUserEntity user = (SysUserEntity) subject.getPrincipal();
			subject.getSession().setAttribute(WebConfig.LOGIN_USER, user);
			LoginRecordEntity loginRecord = new LoginRecordEntity();
			WebConfig.add(loginRecord);
			loginRecord.setC_loginIp(super.getIP(getRequest()));
			loginRecord.setC_username(user.getC_username());
			//采用消息中心的通知添加
			rabbitSender.sendLoginRecord(loginRecord);
			//清楚错误次数缓存
			userService.removeCount(user.getC_username());
			msg = "登陆成功！";
			map.put("flag", flag);
		} catch (Exception exception) {
			if (exception instanceof UnknownAccountException) {
				logger.info("账号不存在： -- > UnknownAccountException");
				msg = "登录失败，用户账号不存在！";
			} else if (exception instanceof IncorrectCredentialsException) {
				logger.info(" 密码不正确： -- >IncorrectCredentialsException");
				msg = "登录失败，用户密码不正确！";
			} else if (exception instanceof LockedAccountException) {
				logger.info(" 用户被锁定： -- >LockedAccountException");
				msg = "登录失败，用户被锁定！";
			} else if (exception instanceof DisabledAccountException) {
				logger.info(" 由于密码输入错误次数大于5次，帐号已经禁止登录！ -- >DisabledAccountException");
				msg = "由于密码输入错误次数大于5次，请5分钟之后登录！";
			}else {
				logger.info("else -- >" + exception);
				msg = "登录失败，发生未知错误：" + exception;
			}
			map.put("flag", false);
		} finally {
			map.put("msg", msg);
			return map;
		}
	}

}
