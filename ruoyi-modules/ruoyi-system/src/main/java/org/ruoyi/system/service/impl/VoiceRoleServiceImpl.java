package org.ruoyi.system.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.system.domain.VoiceRole;
import org.ruoyi.system.domain.bo.VoiceRoleBo;
import org.ruoyi.system.domain.vo.VoiceRoleVo;
import org.ruoyi.system.mapper.VoiceRoleMapper;
import org.ruoyi.system.request.RoleListDto;
import org.ruoyi.system.request.RoleRequest;
import org.ruoyi.system.request.SimpleGenerateRequest;
import org.ruoyi.system.response.RoleResponse;
import org.ruoyi.system.response.SimpleGenerateDataResponse;
import org.ruoyi.system.response.SimpleGenerateResponse;
import org.ruoyi.system.response.rolelist.ContentResponse;
import org.ruoyi.system.response.rolelist.RoleListResponse;
import org.ruoyi.system.response.rolelist.RoleListVO;
import org.ruoyi.system.service.IChatCostService;
import org.ruoyi.system.service.IVoiceRoleService;
import org.ruoyi.system.util.AudioOkHttpUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * 配音角色Service业务层处理
 *
 * @author Lion Li
 * @date 2024-03-19
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class VoiceRoleServiceImpl implements IVoiceRoleService {

    private final VoiceRoleMapper baseMapper;

    private final IChatCostService chatService;

    private final AudioOkHttpUtil audioOkHttpUtil;

    /**
     * 查询配音角色
     */
    @Override
    public VoiceRoleVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询配音角色列表
     */
    @Override
    public TableDataInfo<VoiceRoleVo> queryPageList(VoiceRoleBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<VoiceRole> lqw = buildQueryWrapper(bo);
        Page<VoiceRoleVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询配音角色列表
     */
    @Override
    public List<VoiceRoleVo> queryList(VoiceRoleBo bo) {
        LambdaQueryWrapper<VoiceRole> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<VoiceRole> buildQueryWrapper(VoiceRoleBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<VoiceRole> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), VoiceRole::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), VoiceRole::getDescription, bo.getDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getAvatar()), VoiceRole::getAvatar, bo.getAvatar());
        lqw.eq(bo.getCreateBy()!=null, VoiceRole::getCreateBy, bo.getCreateBy());
        lqw.eq(StringUtils.isNotBlank(bo.getVoiceId()), VoiceRole::getVoiceId, bo.getVoiceId());
        lqw.eq(StringUtils.isNotBlank(bo.getFileUrl()), VoiceRole::getFileUrl, bo.getFileUrl());
        return lqw;
    }

    /**
     * 新增配音角色
     */
    @Override
    public Boolean insertByBo(RoleRequest roleRequest) {
        try {
            String prompt = convertFileToBase64(roleRequest.getPrompt());
            roleRequest.setPrompt("data:audio/x-m4a;base64," + prompt);

            String avatar = convertFileToBase64(roleRequest.getAvatar());
            roleRequest.setAvatar("data:image/png;base64," + avatar);

        } catch (IOException e) {
            log.error("转换base64出现错误：{}", e.getMessage());
        }
        // 创建一个Request对象来配置你的请求
        String json = JSONUtil.toJsonStr(roleRequest);
        Request postRequest = audioOkHttpUtil.createPostRequest("api/tts/voice", json);
        String body = audioOkHttpUtil.executeRequest(postRequest);
        RoleResponse bean = JSONUtil.toBean(body, RoleResponse.class);
        VoiceRole addVoiceRole = new VoiceRole();
        addVoiceRole.setName(roleRequest.getName());
        addVoiceRole.setDescription(roleRequest.getDescription());
        addVoiceRole.setAvatar(bean.getData().getMetadata().getAvatar());
        addVoiceRole.setVoiceId(bean.getData().getId());
        addVoiceRole.setFileUrl(bean.getData().getMetadata().getPrompts().get(0).getPromptOriginAudioStorageUrl());
        return baseMapper.insert(addVoiceRole) > 0;
    }

    private static String convertFileToBase64(String fileUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(fileUrl).build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Failed to download file: " + response);
        byte[] fileData = response.body().bytes();
        return Base64.getEncoder().encodeToString(fileData);
    }

    /**
     * 修改配音角色
     */
    @Override
    public Boolean updateByBo(VoiceRoleBo bo) {
        VoiceRole update = MapstructUtils.convert(bo, VoiceRole.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(VoiceRole entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除配音角色
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 实时生成语音
     *
     * @param simpleGenerateRequest 生成语音对象
     * @return 生成的语音信息
     */
    @Override
    public SimpleGenerateDataResponse simpleGenerate(SimpleGenerateRequest simpleGenerateRequest) {
        double charge = calculateCharge(simpleGenerateRequest.getText());
        // 扣除费用并且保存消息记录
        chatService.taskDeduct(simpleGenerateRequest.getModel(), simpleGenerateRequest.getText(), charge);
        // 创建一个Request对象来配置你的请求
        String json = JSONUtil.toJsonStr(simpleGenerateRequest);
        Request postRequest = audioOkHttpUtil.createPostRequest("api/tts/simple-generate", json);
        String body = audioOkHttpUtil.executeRequest(postRequest);
        SimpleGenerateResponse bean = JSONUtil.toBean(body, SimpleGenerateResponse.class);
        return bean.getData();
    }

    /**
     * 查询市场角色
     *
     * @return 角色列表
     */
    @Override
    public List<RoleListVO> roleList() {
        Request postRequest = audioOkHttpUtil.createGetRequest("api/tts/voice");
        String body = audioOkHttpUtil.executeRequest(postRequest);
        RoleListResponse bean = JSONUtil.toBean(body, RoleListResponse.class);
        List<RoleListVO> roleList = new ArrayList<>();
        for (ContentResponse element : bean.getData()) {
            String name = element.getName();
            String description = element.getMetadata().getDescription();
            String voicesId = element.getId();
            String avatar = element.getMetadata().getAvatar();
            String previewAudio;
            if (element.getMetadata().getPrompts() == null) {
                // 从JSON中解析出的数据没有prompts
                previewAudio = element.getMetadata().getPreviewAudio();
            } else {
                previewAudio = element.getMetadata().getPrompts().get(0).getPromptOriginAudioStorageUrl();
            }
            roleList.add(new RoleListVO(name, description, voicesId, previewAudio, avatar));
        }
        return roleList;

    }

    /**
     * 收藏市场角色
     */
    @Override
    public void copyRole(RoleListDto roleListDto) {
        // 保存至数据库
        VoiceRole voiceRole = new VoiceRole();
        voiceRole.setName(roleListDto.getName());
        voiceRole.setDescription(roleListDto.getDescription());
        voiceRole.setFileUrl(roleListDto.getPreviewAudio());
        voiceRole.setVoiceId(roleListDto.getVoicesId());
        voiceRole.setAvatar(roleListDto.getAvatar());
        baseMapper.insert(voiceRole);
    }

    /**
     * 根据文本长度计算扣除的金额。
     *
     * @param text 输入的文本
     * @return 扣除的金额
     */
    public static double calculateCharge(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }

        int length = text.length();
        double charge = 0.0;

        while (length > 0) {
            if (length >= 500) {
                // 对于每500个字符，扣除0.5元
                charge += (length / 500) * 0.5;
                length %= 500; // 处理剩余字符
            } else if (length >= 100) {
                // 对于100到499个字符，扣除0.2元
                charge += 0.2;
                break; // 处理完毕，退出循环
            } else {
                // 对于少于100个字符，扣除0.1元
                charge += 0.1;
                break; // 处理完毕，退出循环
            }
        }
        return charge;
    }

    public Long getUserId() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            throw new BaseException("用户未登录！");
        }
        return loginUser.getUserId();
    }
}
