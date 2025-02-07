package org.ruoyi.system.domain.vo;

import lombok.Data;
import org.ruoyi.common.core.domain.model.LoginUser;

/**
 * 登录返回信息
 *
 * @author Michelle.Chung
 */
@Data
public class LoginVo {
    private String token;
    private LoginUser userInfo;
}
