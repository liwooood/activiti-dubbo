package com.eloancn.framework.activiti.web.demo.productfactory;

import com.alibaba.fastjson.JSON;
import com.eloancn.framework.activiti.TaskResult;
import com.eloancn.framework.activiti.dto.WorkFlowDto;
import com.eloancn.framework.activiti.facade.ElActiviti;
import com.eloancn.framework.activiti.util.Page;
import com.eloancn.framework.activiti.util.PageUtil;
import com.eloancn.framework.activiti.util.Variable;
import com.eloancn.framework.activiti.util.demo.ActivitiUtils;
import com.eloancn.organ.common.BusCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * &#x8bf7;&#x5047;&#x63a7;&#x5236;&#x5668;&#xff0c;&#x5305;&#x542b;&#x4fdd;&#x5b58;&#x3001;&#x542f;&#x52a8;&#x6d41;&#x7a0b;
 *
 * @author xvshu
 */
@Controller
@RequestMapping(value = "/demo/activitiPF")
public class ActivitProductFactoryDemoController {

    private Logger logger = LoggerFactory.getLogger(getClass());



    @Autowired
    private ElActiviti elActiviti;

    private final String processDefinitionKey = "ProductFactoryNewProduct";

    @RequestMapping(value = {"apply", ""})
    public String createForm(Model model) {
        return "/demo/productfactory/apply";
    }

    /**
     * 启动流程demo
     */
    @RequestMapping(value = "start", method = RequestMethod.POST)
    public String startWorkflow(RedirectAttributes redirectAttributes, HttpSession session,String nextUsers) {

        logger.info("=ActivitMulDemoController.startWorkflow=> nextUsers:{}",nextUsers);

        try {
            String[] nUsers =null;
            String userId = ActivitiUtils.getUserIDFromSession(session);
            if(nextUsers!=null &&nextUsers.length()>0){
                nUsers = nextUsers.split(",");
            }
            List<String> nextUserIds = new ArrayList<>();
            nextUserIds.addAll(Arrays.asList(nUsers));
            WorkFlowDto workFlowDto = new WorkFlowDto(processDefinitionKey,null,userId,String.valueOf(System.currentTimeMillis()),nextUserIds, null, BusCodeEnum.LI_CAI);
            elActiviti.startWorkflow(processDefinitionKey,userId,String.valueOf(System.currentTimeMillis()),nextUserIds, null, BusCodeEnum.LI_CAI);
            redirectAttributes.addFlashAttribute("message", "流程已提交请求");
        } catch (Exception e) {
            if (e.getMessage().indexOf("no processes deployed with key") != -1) {
                logger.warn("没有部署流程!", e);
                redirectAttributes.addFlashAttribute("error", "没有部署流程，请在[工作流]->[流程管理]页面点击<重新部署流程>");
            } else {
                logger.error("启动请假流程失败：", e);
                redirectAttributes.addFlashAttribute("error", "系统内部错误！");
            }
        }
        return "redirect:/demo/activitiweb/apply";
    }




    /**
     * 任务列表
     *
     */
    @RequestMapping(value = "list/task")
    public ModelAndView taskList(HttpSession session, HttpServletRequest request) {

        String userId = ActivitiUtils.getUserIDFromSession(session);

        ModelAndView mav = new ModelAndView("/demo/productfactory/taskList");
        Page<TaskResult> page = new Page<TaskResult>(PageUtil.PAGE_SIZE);
        int[] pageParams = PageUtil.init(page, request);
        List<String> processDefinitionKeys = new ArrayList<String>();
        processDefinitionKeys.add(processDefinitionKey);
        page = elActiviti.findTodoTasks(processDefinitionKeys,userId,page, BusCodeEnum.LI_CAI);

        mav.addObject("page", page);
        return mav;
    }

    /**
     * 读取运行中的流程实例
     *
     * @return
     */
    @RequestMapping(value = "list/running")
    public ModelAndView runningList(HttpSession session,HttpServletRequest request) {

        String userId = ActivitiUtils.getUserIDFromSession(session);

        ModelAndView mav = new ModelAndView("/demo/productfactory/running");
        Page<TaskResult> page = new Page<TaskResult>(PageUtil.PAGE_SIZE);
        PageUtil.init(page, request);
        page = elActiviti.findRunningProcessInstaces(processDefinitionKey,userId, page, BusCodeEnum.LI_CAI);

        mav.addObject("page", page);
        return mav;
    }

    /**
     * 读取结束的流程实例
     *
     * @return
     */
    @RequestMapping(value = "list/finished")
    public ModelAndView finishedList(HttpSession session,HttpServletRequest request) {

        String userId = ActivitiUtils.getUserIDFromSession(session);
        ModelAndView mav = new ModelAndView("/demo/productfactory/finished");
        Page<TaskResult> page = new Page<TaskResult>(PageUtil.PAGE_SIZE);
        PageUtil.init(page, request);
        page = elActiviti.findFinishedProcessInstaces(processDefinitionKey,userId,page, BusCodeEnum.LI_CAI);
        mav.addObject("page", page);
        return mav;
    }

    /**
     * 签收任务
     */
    @RequestMapping(value = "task/claim/{id}")
    public String claim(@PathVariable("id") String taskId, HttpSession session, RedirectAttributes redirectAttributes) {
        String userId = ActivitiUtils.getUserIDFromSession(session);
        elActiviti.claim(taskId, userId,BusCodeEnum.LI_CAI);
        redirectAttributes.addFlashAttribute("message", "任务已签收");
        return "redirect:/demo/activitiweb/list/task";
    }



    /**
     * 完成任务
     *
     * @return
     */
    @RequestMapping(value = "complete/{id}", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public String complete(@PathVariable("id") String taskId, Variable var,HttpSession session) {
        try {
            Map<String, Object> variables = var.getVariableMap();
            String userId = ActivitiUtils.getUserIDFromSession(session);
            String nextUserId = (String)variables.get("nextUserId");

            String[] nUsers =null;
            if(nextUserId!=null &&nextUserId.length()>0){
                nUsers = nextUserId.split(",");
            }
            List<String> nextUserIds = new ArrayList<>();
            nextUserIds.addAll(Arrays.asList(nUsers));

            nextUserIds.add(nextUserId);
            logger.info("=ActivitMulDemoController.complete=>taskId:{} nextUserId:{} variables:{}",taskId,JSON.toJSONString(nextUserId), JSON.toJSONString(variables));

            elActiviti.nodeComplete(userId,taskId, variables,nextUserIds, BusCodeEnum.LI_CAI);
            return "success";
        } catch (Exception e) {
            logger.error("=ActivitMulDemoControllercomplete=>error on complete task {}, variables={}", new Object[]{taskId, var.getVariableMap(), e});
            return "error";
        }
    }


}
