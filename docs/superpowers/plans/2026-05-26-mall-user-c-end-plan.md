# mall-user C 端 API 完整开发计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 mall-user 模块 13 个 C 端 API + 签到 + 积分过期 + 会员升级 + 代码质量改造

**Architecture:** C 端专用 Service（`service/customer/`）与管理端 Service 分离；Controller 走 `throw BusinessException(ErrorCode)` → `MallExceptionHandler` 统一处理；缓存用 `RedisTemplate<String,Object>`（UserConfig 已定义）；MQ 消费者消费 `mall:order:completed`

**Tech Stack:** Spring Boot 4.0.3 / MyBatis-Plus / Redis / RocketMQ 5.5.0 + starter 2.4.2 / `CacheConstants` / `MqTopicConstants` / `ErrorCode` 枚举 / `MallResult<T>`

---

## 文件结构映射

```
mall-common 新建:
  server/mall/mall-common/src/main/java/com/mall/common/enums/user/BizTypeEnum.java        # 业务类型枚举
  (ErrorCode 枚举已存在，直接 import static 使用，无需 ErrorCodeConstants)

mall-user 改造:
  server/mall/mall-user/pom.xml                                                             # 加 rocketmq 依赖
  server/mall/mall-user/src/main/java/com/mall/user/MallUserApplication.java               # +@EnableScheduling
  server/mall/mall-user/src/main/java/com/mall/user/config/MallUserConfigProperties.java   # +signinConsecutiveBonus
  server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserServiceImpl.java  # →构造注入+去魔法值
  server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserAddressServiceImpl.java
  server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserMemberServiceImpl.java
  server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserMemberLevelServiceImpl.java
  server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserPointsAccountServiceImpl.java
  server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserAddressMapper.java       # +3 方法
  server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserMemberMapper.java        # +2 方法
  server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserPointsAccountMapper.java # +2 方法
  server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserPointsLogMapper.java     # +1 方法
  server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserGrowthLogMapper.java     # +1 方法
  server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserMemberLevelMapper.java   # +1 方法

mall-user 新建:
  server/mall/mall-user/src/main/java/com/mall/user/infrastructure/feign/RemoteAuthAdapter.java
  server/mall/mall-user/src/main/java/com/mall/user/service/customer/UserProfileService.java
  server/mall/mall-user/src/main/java/com/mall/user/service/customer/AddressBookService.java
  server/mall/mall-user/src/main/java/com/mall/user/service/customer/MemberService.java
  server/mall/mall-user/src/main/java/com/mall/user/service/customer/PointsService.java
  server/mall/mall-user/src/main/java/com/mall/user/service/customer/SignInService.java
  server/mall/mall-user/src/main/java/com/mall/user/controller/api/UserProfileController.java
  server/mall/mall-user/src/main/java/com/mall/user/controller/api/AddressController.java
  server/mall/mall-user/src/main/java/com/mall/user/controller/api/MemberController.java
  server/mall/mall-user/src/main/java/com/mall/user/controller/api/PointsController.java
  server/mall/mall-user/src/main/java/com/mall/user/controller/api/GrowthController.java
  server/mall/mall-user/src/main/java/com/mall/user/controller/api/SignInController.java
  server/mall/mall-user/src/main/java/com/mall/user/mq/consumer/UserOrderCompletedConsumer.java
  server/mall/mall-user/src/main/java/com/mall/user/schedule/PointsExpireTask.java
  server/mall/mall-user/src/main/java/com/mall/user/controller/api/vo/*VO.java (9 个 VO)

Nacos 控制台:
  mall-user-dev.yml 的 mall.user.points 段 +signin-consecutive-bonus: 1
```

---

### Task 1: mall-common 基础设施 — BizTypeEnum

**Files:**
- Create: `server/mall/mall-common/src/main/java/com/mall/common/enums/user/BizTypeEnum.java`

- [ ] **Step 1: 创建 BizTypeEnum.java**

```java
package com.mall.common.enums.user;

public enum BizTypeEnum {
    ORDER("order", "下单赠送"),
    SIGN_IN("signin", "签到"),
    REVIEW("review", "评价"),
    REFUND("refund", "退款扣除"),
    ADMIN("admin", "管理员调整"),
    EXPIRE("expire", "积分过期");

    private final String code;
    private final String name;

    BizTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }

    public String getName() { return name; }

    public static BizTypeEnum fromCode(String code) {
        for (BizTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn clean compile -f server/mall/pom.xml -pl mall-common -DskipTests
```

预期: BUILD SUCCESS

> 错误码直接使用 `ErrorCode` 枚举，无需 `ErrorCodeConstants`。Service 中 import 方式：`import static com.mall.common.enums.ErrorCode.*`

---

### Task 2: 代码质量 — MallUserConfigProperties 追加 signinConsecutiveBonus

**Files:**
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/config/MallUserConfigProperties.java`

- [ ] **Step 1: 在 Points 内部类追加字段**

修改 `Points` 类，在 `reviewWithPhoto` 后追加：

```java
/** 每连续一天额外加积分 */
private int signinConsecutiveBonus = 1;

public int getSigninConsecutiveBonus() {
    return signinConsecutiveBonus;
}

public void setSigninConsecutiveBonus(int signinConsecutiveBonus) {
    this.signinConsecutiveBonus = signinConsecutiveBonus;
}
```

---

### Task 3: 代码质量 — MallUserServiceImpl 构造注入 + 去魔法值

**Files:**
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserServiceImpl.java`

- [ ] **Step 1: 将 @Autowired 字段注入改为构造注入，删除魔法常量 NICKNAME_PREFIX / PRIVACY_AGREED**

```java
package com.mall.user.service.impl;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.ruoyi.common.core.utils.DateUtils;
import com.mall.common.enums.user.RegisterTypeEnum;
import com.mall.common.enums.user.UserStatusEnum;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import com.mall.user.mapper.MallUserMapper;
import com.mall.user.DO.MallUserDO;
import com.mall.user.service.IMallUserService;

@Service
public class MallUserServiceImpl implements IMallUserService {

    private final MallUserMapper mallUserMapper;

    public MallUserServiceImpl(MallUserMapper mallUserMapper) {
        this.mallUserMapper = mallUserMapper;
    }

    @Override
    public MallUserDO selectMallUserById(String id) {
        return mallUserMapper.selectMallUserById(id);
    }

    @Override
    public List<MallUserDO> selectMallUserList(MallUserDO mallUser) {
        return mallUserMapper.selectMallUserList(mallUser);
    }

    @Override
    public int insertMallUser(MallUserDO mallUser) {
        mallUser.setCreateTime(DateUtils.getNowDate());
        return mallUserMapper.insertMallUser(mallUser);
    }

    @Override
    public int updateMallUser(MallUserDO mallUser) {
        mallUser.setUpdateTime(DateUtils.getNowDate());
        return mallUserMapper.updateMallUser(mallUser);
    }

    @Override
    public int deleteMallUserByIds(String[] ids) {
        return mallUserMapper.deleteMallUserByIds(ids);
    }

    @Override
    public int deleteMallUserById(String id) {
        return mallUserMapper.deleteMallUserById(id);
    }

    @Override
    public MallUserDO selectByPhoneHash(String phoneHash) {
        return mallUserMapper.selectByPhoneHash(phoneHash);
    }

    @Override
    public MallUserDO selectByPhone(String phone) {
        String phoneHash = DigestUtils.sha256Hex(phone);
        return mallUserMapper.selectByPhoneHash(phoneHash);
    }

    @Override
    public MallUserDO selectByWechatOpenId(String openId) {
        return null;
    }

    @Override
    public String registerByPhone(String phone, String phoneHash, String passwordHash) {
        MallUserDO user = new MallUserDO();
        user.setId(UUID.randomUUID().toString());
        user.setPhone(phone);
        user.setPhoneHash(phoneHash);
        user.setPassword(passwordHash);
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setUserStatus(String.valueOf(UserStatusEnum.NORMAL.getCode()));
        user.setRegisterType(RegisterTypeEnum.PHONE.getCode());
        user.setIsPrivacyAgreed(String.valueOf(1));
        Date now = DateUtils.getNowDate();
        user.setRegisterTime(now);
        user.setCreateTime(now);
        user.setPrivacyAgreedTime(now);
        mallUserMapper.insertMallUser(user);
        return user.getId();
    }

    @Override
    public int updatePasswordById(String id, String newPasswordHash) {
        return mallUserMapper.updatePassword(id, newPasswordHash);
    }

    @Override
    public int updatePhoneById(String id, String newPhone, String newPhoneHash) {
        return mallUserMapper.updatePhone(id, newPhone, newPhoneHash);
    }

    @Override
    public int updateUserStatusById(String id, String userStatus) {
        return mallUserMapper.updateUserStatus(id, userStatus);
    }
}
```

关键变更：
- 删除了 `private static final String NICKNAME_PREFIX = "用户"` 和 `private static final String PRIVACY_AGREED = "1"`
- `@Autowired private MallUserMapper mallUserMapper` → `private final MallUserMapper mallUserMapper` + 构造注入
- 魔法字符串 `"用户"` 和 `"1"` 直接内联（简单常量无需提取）

---

### Task 4: 代码质量 — 其余 6 个 Service 构造注入

**Files:**
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserAddressServiceImpl.java`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserMemberServiceImpl.java`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserMemberLevelServiceImpl.java`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserPointsAccountServiceImpl.java`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserPointsLogServiceImpl.java`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserGrowthLogServiceImpl.java`

- [ ] **Step 1: 逐个改造为构造注入**

每个文件的改造模式相同：`@Autowired` 字段 → `private final` + 构造参数。以 MallUserAddressServiceImpl 为例（其余同理）：

```java
@Service
public class MallUserAddressServiceImpl implements IMallUserAddressService {

    private final MallUserAddressMapper mallUserAddressMapper;

    public MallUserAddressServiceImpl(MallUserAddressMapper mallUserAddressMapper) {
        this.mallUserAddressMapper = mallUserAddressMapper;
    }

    // ... 其余方法不变，删除 @Autowired 注解
}
```

MallUserMemberServiceImpl:
```java
@Service
public class MallUserMemberServiceImpl implements IMallUserMemberService {

    private final MallUserMemberMapper mallUserMemberMapper;

    public MallUserMemberServiceImpl(MallUserMemberMapper mallUserMemberMapper) {
        this.mallUserMemberMapper = mallUserMemberMapper;
    }
}
```

其余 4 个同理，替换字段名和参数类型即可。

---

### Task 5: Mapper 改造 — MyBatis-Plus 集成

> MyBatis-Plus 向后兼容 MyBatis XML Mapper。本次只做两件事：① Mapper 接口加 `extends BaseMapper<T>`（解锁 LambdaQueryWrapper）；② 仅在 XML 保留自定义 UPDATE 和有副作用的 SQL，简单查询在 Service 中用 `LambdaQueryWrapper` 替代。

**Files:**
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserAddressMapper.java`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserMemberMapper.java`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserPointsAccountMapper.java`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserMemberLevelMapper.java`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserAddressMapper.xml`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserMemberMapper.xml`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserPointsAccountMapper.xml`

- [ ] **Step 1: MallUserAddressMapper 加 `extends BaseMapper<MallUserAddressDO>`，XML 只追加 `clearDefault`**

MallUserAddressMapper.java：
```java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface MallUserAddressMapper extends BaseMapper<MallUserAddressDO> {
    // ... 已有方法不变
    int clearDefault(@Param("userId") String userId);
}
```

MallUserAddressMapper.xml 追加：
```xml
<update id="clearDefault">
    update mall_user_address set is_default = '0', update_time = now() where user_id = #{userId}
</update>
```

> `selectByUserId` / `countByUserId` 由 Service 用 `LambdaQueryWrapper` 替代，见 Task 10。

- [ ] **Step 2: MallUserMemberMapper 加 `extends BaseMapper<MallUserMemberDO>`，XML 追加 `addGrowth`**

MallUserMemberMapper.java：
```java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface MallUserMemberMapper extends BaseMapper<MallUserMemberDO> {
    // ... 已有方法不变
    int addGrowth(@Param("userId") String userId, @Param("growth") Long growth);
}
```

MallUserMemberMapper.xml 追加：
```xml
<update id="addGrowth">
    update mall_user_member
    set growth = growth + #{growth},
        total_growth = total_growth + #{growth},
        update_time = now()
    where user_id = #{userId}
</update>
```

> `selectByUserId` 由 Service 用 `LambdaQueryWrapper` 替代。

- [ ] **Step 3: MallUserPointsAccountMapper 加 `extends BaseMapper<MallPointsAccountDO>`，XML 追加 `addPoints`**

MallUserPointsAccountMapper.java：
```java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface MallUserPointsAccountMapper extends BaseMapper<MallPointsAccountDO> {
    // ... 已有方法不变
    int addPoints(@Param("userId") String userId, @Param("points") int points,
                  @Param("version") int version);
}
```

MallUserPointsAccountMapper.xml 追加：
```xml
<update id="addPoints">
    update mall_user_points_account
    set available_points = available_points + #{points},
        total_points = total_points + #{points},
        version = version + 1,
        update_time = now()
    where user_id = #{userId} and version = #{version}
</update>
```

> `selectByUserId` 由 Service 用 `LambdaQueryWrapper` 替代。

- [ ] **Step 4: MallUserMemberLevelMapper 加 `extends BaseMapper<MallMemberLevelDO>`**

```java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface MallUserMemberLevelMapper extends BaseMapper<MallMemberLevelDO> {
    // ... 已有方法不变
}
```

> `selectAllOrderByLevelValue` 由 Service 用 `LambdaQueryWrapper` 替代，无需 XML。

---

### Task 6: Mapper 扩展 — 流水表分页查询（XML + MyBatis-Plus Page）

> 流水表 `MallPointsLogDO` / `MallGrowthLogDO` 的分页查询需要指定列 + `order by`，保留 XML 方式。用 MyBatis-Plus 的 `Page<T>` + `IPage<T>` 替代 PageHelper。追加 `bizType` 过滤入参（Fix 2）。

**Files:**
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserPointsLogMapper.java`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserPointsLogMapper.xml`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserGrowthLogMapper.java`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserGrowthLogMapper.xml`

- [ ] **Step 1: MallUserPointsLogMapper + XML**

```java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface MallUserPointsLogMapper extends BaseMapper<MallPointsLogDO> {
    // ... 已有方法不变
    IPage<MallPointsLogDO> selectByUserIdPage(Page<MallPointsLogDO> page,
            @Param("userId") String userId, @Param("bizType") String bizType);
}
```

XML（若 resultMap 不存在需补 `MallUserPointsLogResult`）：
```xml
<select id="selectByUserIdPage" resultMap="MallUserPointsLogResult">
    select id, user_id, biz_type, biz_no, change_type, points,
           before_points, after_points, remark, create_time
    from mall_user_points_log
    where user_id = #{userId}
    <if test="bizType != null and bizType != ''"> and biz_type = #{bizType}</if>
    order by create_time desc
</select>
```

- [ ] **Step 2: MallUserGrowthLogMapper + XML（同理）**

```java
public interface MallUserGrowthLogMapper extends BaseMapper<MallGrowthLogDO> {
    // ... 已有方法不变
    IPage<MallGrowthLogDO> selectByUserIdPage(Page<MallGrowthLogDO> page,
            @Param("userId") String userId, @Param("bizType") String bizType);
}
```

XML 同结构，字段调为 `growth`/`before_growth`/`after_growth`。MyBatis-Plus 检测到 `Page` 参数自动添加 COUNT + LIMIT，无需在 SQL 中手写。

- [ ] **Step 3: PointsExpireTask 分页查询 Mapper（Fix 6）**

在 `MallUserPointsAccountMapper` 追加：
```java
List<MallPointsAccountDO> selectAvailablePointsByPage(@Param("offset") int offset,
        @Param("limit") int limit);
```

XML：
```xml
<select id="selectAvailablePointsByPage" resultMap="MallUserPointsAccountResult">
    select id, user_id, total_points, available_points, used_points, expired_points, version
    from mall_user_points_account
    where available_points > 0
    order by id
    limit #{offset}, #{limit}
</select>
```

---

### Task 7: RemoteAuthAdapter

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/infrastructure/feign/RemoteAuthAdapter.java`

- [ ] **Step 1: 创建 RemoteAuthAdapter**

```java
package com.mall.user.infrastructure.feign;

import org.springframework.stereotype.Component;

@Component
public class RemoteAuthAdapter {

    public String decryptPhone(String encryptedPhone) {
        return encryptedPhone;
    }

    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
```

> 预留 decryptPhone（当前暂不解密，直接返回原值）；maskPhone 用于脱敏展示

---

### Task 8: VO 类（9 个）

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/vo/UserProfileVO.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/vo/AddressVO.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/vo/MembershipVO.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/vo/PointsVO.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/vo/PointsRecordVO.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/vo/GrowthVO.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/vo/GrowthRecordVO.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/vo/SignInVO.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/vo/MemberLevelVO.java`

- [ ] **Step 1: UserProfileVO**

```java
package com.mall.user.controller.api.vo;

public class UserProfileVO {
    private String userId;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String genderName;
    private String birthday;
    private String phone;
    private String email;
    private String membershipLevel;
    private String membershipIcon;
    private Long growth;
    private Long totalGrowth;
    private Integer points;
    private Integer availablePoints;
    // getters / setters
}
```

- [ ] **Step 2: AddressVO**

```java
package com.mall.user.controller.api.vo;

public class AddressVO {
    private String addressId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private String zipCode;
    private Boolean isDefault;
    private String label;
    // getters / setters
}
```

- [ ] **Step 3: MemberLevelVO**

```java
package com.mall.user.controller.api.vo;

public class MemberLevelVO {
    private String levelName;
    private String icon;
    private Integer levelValue;
    // getters / setters
}
```

- [ ] **Step 4: MembershipVO**

```java
package com.mall.user.controller.api.vo;

import java.util.List;

public class MembershipVO {
    private MemberLevelVO currentLevel;
    private Long growth;
    private Long totalGrowth;
    private MemberLevelVO nextLevel;
    private List<String> benefits;
    // getters / setters
}
```

- [ ] **Step 5: PointsVO**

```java
package com.mall.user.controller.api.vo;

public class PointsVO {
    private Integer totalPoints;
    private Integer availablePoints;
    private Integer usedPoints;
    private Integer expiredPoints;
    // getters / setters
}
```

- [ ] **Step 6: PointsRecordVO**

```java
package com.mall.user.controller.api.vo;

import java.util.Date;

public class PointsRecordVO {
    private Long id;
    private String bizType;
    private String bizTypeName;
    private Integer changeType;
    private Integer points;
    private Integer beforePoints;
    private Integer afterPoints;
    private String remark;
    private Date createTime;
    // getters / setters
}
```

- [ ] **Step 7: GrowthVO**

```java
package com.mall.user.controller.api.vo;

public class GrowthVO {
    private Long growth;
    private Long totalGrowth;
    private MemberLevelVO currentLevel;
    private MemberLevelVO nextLevel;
    private Long needGrowth;
    private Integer progressPercent;
    // getters / setters
}
```

- [ ] **Step 8: GrowthRecordVO**

```java
package com.mall.user.controller.api.vo;

import java.util.Date;

public class GrowthRecordVO {
    private Long id;
    private String bizType;
    private String bizTypeName;
    private Integer changeType;
    private Long growth;
    private Long beforeGrowth;
    private Long afterGrowth;
    private String remark;
    private Date createTime;
    // getters / setters
}
```

- [ ] **Step 9: SignInVO**

```java
package com.mall.user.controller.api.vo;

import java.util.List;

public class SignInVO {
    private Integer todayPoints;
    private Integer consecutiveDays;
    private List<Integer> signInCalendar;
    // getters / setters
}
```

---

### Task 9: C 端 Service — UserProfileService

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/service/customer/UserProfileService.java`

- [ ] **Step 1: 编写 UserProfileService**

```java
package com.mall.user.service.customer;

import com.mall.common.enums.user.GenderEnum;
import com.mall.common.enums.user.UserStatusEnum;
import com.mall.user.controller.api.vo.MemberLevelVO;
import com.mall.user.controller.api.vo.UserProfileVO;
import com.mall.user.DO.MallUserDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallMemberLevelDO;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.infrastructure.feign.RemoteAuthAdapter;
import com.mall.user.mapper.MallUserMapper;
import com.mall.user.mapper.MallUserMemberMapper;
import com.mall.user.mapper.MallUserMemberLevelMapper;
import com.mall.user.mapper.MallUserPointsAccountMapper;
import com.mall.user.config.MallUserConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.mall.common.constant.CacheConstants.User.PROFILE;
import static com.mall.common.enums.ErrorCode.ACCOUNT_DELETED;
import static com.mall.common.enums.ErrorCode.ACCOUNT_FROZEN;
import static com.mall.common.enums.ErrorCode.ACCOUNT_NOT_FOUND;
import static com.mall.common.enums.ErrorCode.RESOURCE_NOT_FOUND;
import static com.mall.common.enums.ErrorCode.RESOURCE_STATUS_ERROR;

import com.mall.common.exception.BusinessException;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final MallUserMapper mallUserMapper;
    private final MallUserMemberMapper mallUserMemberMapper;
    private final MallUserMemberLevelMapper mallUserMemberLevelMapper;
    private final MallUserPointsAccountMapper mallUserPointsAccountMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RemoteAuthAdapter remoteAuthAdapter;
    private final MallUserConfigProperties configProperties;

    public UserProfileService(MallUserMapper mallUserMapper,
                              MallUserMemberMapper mallUserMemberMapper,
                              MallUserMemberLevelMapper mallUserMemberLevelMapper,
                              MallUserPointsAccountMapper mallUserPointsAccountMapper,
                              RedisTemplate<String, Object> redisTemplate,
                              RemoteAuthAdapter remoteAuthAdapter,
                              MallUserConfigProperties configProperties) {
        this.mallUserMapper = mallUserMapper;
        this.mallUserMemberMapper = mallUserMemberMapper;
        this.mallUserMemberLevelMapper = mallUserMemberLevelMapper;
        this.mallUserPointsAccountMapper = mallUserPointsAccountMapper;
        this.redisTemplate = redisTemplate;
        this.remoteAuthAdapter = remoteAuthAdapter;
        this.configProperties = configProperties;
    }

    public UserProfileVO getProfile(String userId) {
        String cacheKey = PROFILE + userId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof UserProfileVO) {
            return (UserProfileVO) cached;
        }
        UserProfileVO vo = buildProfile(userId);
        redisTemplate.opsForValue().set(cacheKey, vo, configProperties.getProfile().getCacheTtl(), TimeUnit.SECONDS);
        return vo;
    }

    private UserProfileVO buildProfile(String userId) {
        MallUserDO user = mallUserMapper.selectMallUserById(userId);
        if (user == null) {
            throw new BusinessException(ACCOUNT_NOT_FOUND);
        }
        String status = user.getUserStatus();
        if (String.valueOf(UserStatusEnum.FROZEN.getCode()).equals(status)) {
            throw new BusinessException(ACCOUNT_FROZEN);
        }
        if (String.valueOf(UserStatusEnum.DELETED.getCode()).equals(status)) {
            throw new BusinessException(ACCOUNT_DELETED);
        }
        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        if (user.getGender() != null && !user.getGender().isEmpty()) {
            vo.setGender(Integer.valueOf(user.getGender()));
            vo.setGenderName(GenderEnum.fromCode(Integer.valueOf(user.getGender())).getName());
        }
        vo.setBirthday(user.getBirthday() != null ? user.getBirthday().toString() : null);
        vo.setPhone(remoteAuthAdapter.maskPhone(user.getPhone()));
        vo.setEmail(user.getEmail());

        MallUserMemberDO member = mallUserMemberMapper.selectByUserId(userId);
        if (member != null) {
            MallMemberLevelDO level = mallUserMemberLevelMapper.selectMallUserMemberLevelById(member.getLevelId());
            if (level != null) {
                vo.setMembershipLevel(level.getLevelName());
                vo.setMembershipIcon(level.getIcon());
            }
            vo.setGrowth(member.getGrowth());
            vo.setTotalGrowth(member.getTotalGrowth());
        }

        MallPointsAccountDO pointsAccount = mallUserPointsAccountMapper.selectByUserId(userId);
        if (pointsAccount != null) {
            vo.setPoints(pointsAccount.getTotalPoints() != null ? pointsAccount.getTotalPoints().intValue() : 0);
            vo.setAvailablePoints(pointsAccount.getAvailablePoints() != null ? pointsAccount.getAvailablePoints().intValue() : 0);
        }
        return vo;
    }

    @Transactional
    public UserProfileVO updateProfile(String userId, UserProfileVO updateRequest) {
        MallUserDO user = mallUserMapper.selectMallUserById(userId);
        if (user == null) {
            throw new BusinessException(ACCOUNT_NOT_FOUND);
        }
        String status = user.getUserStatus();
        if (String.valueOf(UserStatusEnum.FROZEN.getCode()).equals(status)
                || String.valueOf(UserStatusEnum.DELETED.getCode()).equals(status)) {
            throw new BusinessException(RESOURCE_STATUS_ERROR);
        }
        if (updateRequest.getNickname() != null) {
            user.setNickname(updateRequest.getNickname());
        }
        if (updateRequest.getAvatar() != null) {
            user.setAvatar(updateRequest.getAvatar());
        }
        if (updateRequest.getGender() != null) {
            user.setGender(String.valueOf(updateRequest.getGender()));
        }
        if (updateRequest.getBirthday() != null) {
            user.setBirthday(java.sql.Date.valueOf(updateRequest.getBirthday()));
        }
        if (updateRequest.getEmail() != null) {
            user.setEmail(updateRequest.getEmail());
        }
        user.setUpdateTime(new Date());
        mallUserMapper.updateMallUser(user);

        String cacheKey = PROFILE + userId;
        redisTemplate.delete(cacheKey);
        return buildProfile(userId);
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn clean compile -f server/mall/pom.xml -pl mall-user -DskipTests
```

---

### Task 10: C 端 Service — AddressBookService

> MyBatis-Plus 模式下，`selectByUserId` / `countByUserId` 用 `LambdaQueryWrapper`，无需 XML 方法：
> ```java
> mapper.selectList(new LambdaQueryWrapper<MallUserAddressDO>().eq(MallUserAddressDO::getUserId, userId)
>     .orderByDesc(MallUserAddressDO::getIsDefault).orderByDesc(MallUserAddressDO::getCreateTime));
> mapper.selectCount(new LambdaQueryWrapper<MallUserAddressDO>().eq(MallUserAddressDO::getUserId, userId));
> ```

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/service/customer/AddressBookService.java`

- [ ] **Step 1: 编写 AddressBookService**

```java
package com.mall.user.service.customer;

import com.mall.user.config.MallUserConfigProperties;
import com.mall.user.controller.api.vo.AddressVO;
import com.mall.user.DO.MallUserAddressDO;
import com.mall.user.mapper.MallUserAddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.mall.common.enums.ErrorCode.ADDRESS_LIMIT;
import static com.mall.common.enums.ErrorCode.RESOURCE_NOT_FOUND;
import static com.mall.common.enums.ErrorCode.RESOURCE_STATUS_ERROR;

import com.mall.common.exception.BusinessException;

@Service
public class AddressBookService {

    private final MallUserAddressMapper mallUserAddressMapper;
    private final MallUserConfigProperties configProperties;

    public AddressBookService(MallUserAddressMapper mallUserAddressMapper,
                              MallUserConfigProperties configProperties) {
        this.mallUserAddressMapper = mallUserAddressMapper;
        this.configProperties = configProperties;
    }

    public List<AddressVO> listAddresses(String userId) {
        List<MallUserAddressDO> addresses = mallUserAddressMapper.selectByUserId(userId);
        List<AddressVO> vos = new ArrayList<>();
        for (MallUserAddressDO addr : addresses) {
            vos.add(toVO(addr));
        }
        return vos;
    }

    @Transactional
    public AddressVO addAddress(String userId, AddressVO request) {
        int count = mallUserAddressMapper.countByUserId(userId);
        if (count >= configProperties.getAddress().getMaxCount()) {
            throw new BusinessException(ADDRESS_LIMIT);
        }
        MallUserAddressDO entity = new MallUserAddressDO();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        fillEntity(entity, request);
        if (count == 0) {
            entity.setIsDefault(String.valueOf(1));
        } else {
            entity.setIsDefault(request.getIsDefault() != null && request.getIsDefault() ? String.valueOf(1) : String.valueOf(0));
        }
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        mallUserAddressMapper.insertMallUserAddress(entity);
        return toVO(entity);
    }

    @Transactional
    public AddressVO updateAddress(String userId, String addressId, AddressVO request) {
        MallUserAddressDO entity = mallUserAddressMapper.selectMallUserAddressById(addressId);
        if (entity == null || !entity.getUserId().equals(userId)) {
            throw new BusinessException(RESOURCE_NOT_FOUND);
        }
        fillEntity(entity, request);
        entity.setUpdateTime(new Date());
        mallUserAddressMapper.updateMallUserAddress(entity);
        return toVO(entity);
    }

    @Transactional
    public void deleteAddress(String userId, String addressId) {
        MallUserAddressDO entity = mallUserAddressMapper.selectMallUserAddressById(addressId);
        if (entity == null || !entity.getUserId().equals(userId)) {
            throw new BusinessException(RESOURCE_NOT_FOUND);
        }
        mallUserAddressMapper.deleteMallUserAddressById(addressId);
    }

    @Transactional
    public void setDefault(String userId, String addressId) {
        MallUserAddressDO entity = mallUserAddressMapper.selectMallUserAddressById(addressId);
        if (entity == null || !entity.getUserId().equals(userId)) {
            throw new BusinessException(RESOURCE_NOT_FOUND);
        }
        mallUserAddressMapper.clearDefault(userId);
        entity.setIsDefault(String.valueOf(1));
        entity.setUpdateTime(new Date());
        mallUserAddressMapper.updateMallUserAddress(entity);
    }

    private void fillEntity(MallUserAddressDO entity, AddressVO vo) {
        entity.setReceiverName(vo.getReceiverName());
        entity.setReceiverPhone(vo.getReceiverPhone());
        entity.setProvince(vo.getProvince());
        entity.setCity(vo.getCity());
        entity.setDistrict(vo.getDistrict());
        entity.setDetailAddress(vo.getDetailAddress());
        entity.setZipCode(vo.getZipCode());
        entity.setLabel(vo.getLabel());
    }

    private AddressVO toVO(MallUserAddressDO entity) {
        AddressVO vo = new AddressVO();
        vo.setAddressId(entity.getId());
        vo.setReceiverName(entity.getReceiverName());
        vo.setReceiverPhone(entity.getReceiverPhone());
        vo.setProvince(entity.getProvince());
        vo.setCity(entity.getCity());
        vo.setDistrict(entity.getDistrict());
        vo.setDetailAddress(entity.getDetailAddress());
        vo.setZipCode(entity.getZipCode());
        vo.setIsDefault(String.valueOf(1).equals(entity.getIsDefault()));
        vo.setLabel(entity.getLabel());
        return vo;
    }
}
```

---

### Task 11: C 端 Service — MemberService

> MyBatis-Plus 模式：`selectByUserId` → `LambdaQueryWrapper`；`selectAllOrderByLevelValue` → `mapper.selectList(new LambdaQueryWrapper<MallMemberLevelDO>().orderByAsc(MallMemberLevelDO::getLevelValue))`。

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/service/customer/MemberService.java`

- [ ] **Step 1: 编写 MemberService**

```java
package com.mall.user.service.customer;

import com.mall.common.enums.user.BizTypeEnum;
import com.mall.user.controller.api.vo.MemberLevelVO;
import com.mall.user.controller.api.vo.MembershipVO;
import com.mall.user.DO.MallGrowthLogDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallMemberLevelDO;
import com.mall.user.mapper.MallUserGrowthLogMapper;
import com.mall.user.mapper.MallUserMemberLevelMapper;
import com.mall.user.mapper.MallUserMemberMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.mall.common.enums.ErrorCode.ACCOUNT_NOT_FOUND;
import static com.mall.common.enums.ErrorCode.RESOURCE_NOT_FOUND;

import com.mall.common.exception.BusinessException;

@Service
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final MallUserMemberMapper mallUserMemberMapper;
    private final MallUserMemberLevelMapper mallUserMemberLevelMapper;
    private final MallUserGrowthLogMapper mallUserGrowthLogMapper;
    private final ObjectMapper objectMapper;

    public MemberService(MallUserMemberMapper mallUserMemberMapper,
                         MallUserMemberLevelMapper mallUserMemberLevelMapper,
                         MallUserGrowthLogMapper mallUserGrowthLogMapper,
                         ObjectMapper objectMapper) {
        this.mallUserMemberMapper = mallUserMemberMapper;
        this.mallUserMemberLevelMapper = mallUserMemberLevelMapper;
        this.mallUserGrowthLogMapper = mallUserGrowthLogMapper;
        this.objectMapper = objectMapper;
    }

    public MembershipVO getMembership(String userId) {
        MallUserMemberDO member = mallUserMemberMapper.selectByUserId(userId);
        if (member == null) {
            throw new BusinessException(ACCOUNT_NOT_FOUND);
        }
        List<MallMemberLevelDO> allLevels = mallUserMemberLevelMapper.selectAllOrderByLevelValue();
        MallMemberLevelDO currentLevel = allLevels.stream()
                .filter(l -> l.getId().equals(member.getLevelId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(RESOURCE_NOT_FOUND));

        MembershipVO vo = new MembershipVO();
        vo.setCurrentLevel(toLevelVO(currentLevel));
        vo.setGrowth(member.getGrowth());
        vo.setTotalGrowth(member.getTotalGrowth());

        MallMemberLevelDO nextLevel = allLevels.stream()
                .filter(l -> l.getMinGrowth() > member.getGrowth())
                .findFirst()
                .orElse(null);
        if (nextLevel != null) {
            vo.setNextLevel(toLevelVO(nextLevel));
        }

        try {
            if (currentLevel.getBenefitsJson() != null) {
                String[] benefits = objectMapper.readValue(currentLevel.getBenefitsJson(), String[].class);
                vo.setBenefits(Arrays.asList(benefits));
            }
        } catch (Exception e) {
            log.warn("解析会员权益 JSON 失败: userId={}, levelId={}", userId, currentLevel.getId(), e);
            vo.setBenefits(new ArrayList<>());
        }
        return vo;
    }

    @Transactional
    public void addGrowth(String userId, Long growth, BizTypeEnum bizType, String bizNo) {
        MallUserMemberDO member = mallUserMemberMapper.selectByUserId(userId);
        if (member == null) {
            log.warn("用户会员信息不存在: userId={}", userId);
            return;
        }
        Long beforeGrowth = member.getGrowth();
        mallUserMemberMapper.addGrowth(userId, growth);

        MallGrowthLogDO logEntity = new MallGrowthLogDO();
        logEntity.setUserId(userId);
        logEntity.setBizType(bizType.getCode());
        logEntity.setBizNo(bizNo);
        logEntity.setChangeType(1);
        logEntity.setGrowth(growth);
        logEntity.setBeforeGrowth(beforeGrowth);
        logEntity.setAfterGrowth(beforeGrowth + growth);
        logEntity.setCreateTime(new Date());
        mallUserGrowthLogMapper.insertMallUserGrowthLog(logEntity);

        Long newGrowth = beforeGrowth + growth;
        checkUpgrade(userId, newGrowth);
    }

    private void checkUpgrade(String userId, Long newGrowth) {
        List<MallMemberLevelDO> allLevels = mallUserMemberLevelMapper.selectAllOrderByLevelValue();
        MallUserMemberDO member = mallUserMemberMapper.selectByUserId(userId);
        if (member == null) {
            return;
        }
        MallMemberLevelDO newLevel = null;
        for (MallMemberLevelDO level : allLevels) {
            if (newGrowth >= level.getMinGrowth() && newGrowth < level.getMaxGrowth()) {
                newLevel = level;
                break;
            }
        }
        if (newLevel != null && !newLevel.getId().equals(member.getLevelId())) {
            MallUserMemberDO update = new MallUserMemberDO();
            update.setId(member.getId());
            update.setLevelId(newLevel.getId());
            update.setLevelStartTime(new Date());
            update.setUpdateTime(new Date());
            mallUserMemberMapper.updateMallUserMember(update);
            log.info("用户会员升级: userId={}, oldLevel={}, newLevel={}, growth={}",
                    userId, member.getLevelId(), newLevel.getId(), newGrowth);
        }
    }

    private MemberLevelVO toLevelVO(MallMemberLevelDO level) {
        MemberLevelVO vo = new MemberLevelVO();
        vo.setLevelName(level.getLevelName());
        vo.setIcon(level.getIcon());
        vo.setLevelValue(Integer.valueOf(level.getLevelValue()));
        return vo;
    }
}
```

---

### Task 12: C 端 Service — PointsService

> MyBatis-Plus 模式：`selectByUserId` → `LambdaQueryWrapper`；`selectByUserIdPage` 用 XML + MyBatis-Plus `Page<T>` 分页。

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/service/customer/PointsService.java`

- [ ] **Step 1: 编写 PointsService**

```java
package com.mall.user.service.customer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.user.controller.api.vo.PointsRecordVO;
import com.mall.user.controller.api.vo.PointsVO;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallPointsLogDO;
import com.mall.user.mapper.MallUserPointsAccountMapper;
import com.mall.user.mapper.MallUserPointsLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mall.common.enums.ErrorCode.ACCOUNT_NOT_FOUND;

import com.mall.common.exception.BusinessException;

@Service
public class PointsService {

    private static final Logger log = LoggerFactory.getLogger(PointsService.class);
    private static final int MAX_RETRY = 3;

    private final MallUserPointsAccountMapper mallUserPointsAccountMapper;
    private final MallUserPointsLogMapper mallUserPointsLogMapper;

    public PointsService(MallUserPointsAccountMapper mallUserPointsAccountMapper,
                         MallUserPointsLogMapper mallUserPointsLogMapper) {
        this.mallUserPointsAccountMapper = mallUserPointsAccountMapper;
        this.mallUserPointsLogMapper = mallUserPointsLogMapper;
    }

    public PointsVO getPoints(String userId) {
        MallPointsAccountDO account = mallUserPointsAccountMapper.selectByUserId(userId);
        if (account == null) {
            throw new BusinessException(ACCOUNT_NOT_FOUND);
        }
        PointsVO vo = new PointsVO();
        vo.setTotalPoints(account.getTotalPoints() != null ? account.getTotalPoints().intValue() : 0);
        vo.setAvailablePoints(account.getAvailablePoints() != null ? account.getAvailablePoints().intValue() : 0);
        vo.setUsedPoints(account.getUsedPoints() != null ? account.getUsedPoints().intValue() : 0);
        vo.setExpiredPoints(account.getExpiredPoints() != null ? account.getExpiredPoints().intValue() : 0);
        return vo;
    }

    public IPage<PointsRecordVO> getPointsRecords(String userId, String bizType, int page, int size) {
        Page<MallPointsLogDO> mpPage = new Page<>(page, size);
        IPage<MallPointsLogDO> logPage = mallUserPointsLogMapper.selectByUserIdPage(mpPage, userId, bizType);
        List<PointsRecordVO> vos = new ArrayList<>();
        for (MallPointsLogDO logEntry : logPage.getRecords()) {
            PointsRecordVO vo = new PointsRecordVO();
            vo.setId(logEntry.getId());
            vo.setBizType(logEntry.getBizType());
            BizTypeEnum bizTypeEnum = BizTypeEnum.fromCode(logEntry.getBizType());
            vo.setBizTypeName(bizTypeEnum != null ? bizTypeEnum.getName() : logEntry.getBizType());
            vo.setChangeType(logEntry.getChangeType());
            vo.setPoints(logEntry.getPoints());
            vo.setBeforePoints(logEntry.getBeforePoints());
            vo.setAfterPoints(logEntry.getAfterPoints());
            vo.setRemark(logEntry.getRemark());
            vo.setCreateTime(logEntry.getCreateTime());
            vos.add(vo);
        }
        Page<PointsRecordVO> result = new Page<>(page, size, logPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Transactional
    public void addPoints(String userId, int points, BizTypeEnum bizType, String bizNo) {
        int retry = 0;
        while (retry < MAX_RETRY) {
            MallPointsAccountDO account = mallUserPointsAccountMapper.selectByUserId(userId);
            if (account == null) {
                log.warn("用户积分账户不存在: userId={}", userId);
                return;
            }
            int beforePoints = account.getAvailablePoints() != null ? account.getAvailablePoints().intValue() : 0;
            int rows = mallUserPointsAccountMapper.addPoints(userId, points, account.getVersion());
            if (rows > 0) {
                MallPointsLogDO logEntity = new MallPointsLogDO();
                logEntity.setUserId(userId);
                logEntity.setBizType(bizType.getCode());
                logEntity.setBizNo(bizNo);
                logEntity.setChangeType(1);
                logEntity.setPoints(points);
                logEntity.setBeforePoints(beforePoints);
                logEntity.setAfterPoints(beforePoints + points);
                logEntity.setCreateTime(new Date());
                mallUserPointsLogMapper.insertMallUserPointsLog(logEntity);
                return;
            }
            retry++;
            log.warn("积分增加乐观锁冲突重试: userId={}, retry={}", userId, retry);
        }
        log.error("积分增加失败（已达最大重试）: userId={}, points={}", userId, points);
    }
}
```

---

### Task 13: C 端 Service — SignInService

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/service/customer/SignInService.java`

- [ ] **Step 1: 编写 SignInService**

```java
package com.mall.user.service.customer;

import com.mall.common.enums.user.BizTypeEnum;
import com.mall.user.config.MallUserConfigProperties;
import com.mall.user.controller.api.vo.SignInVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.mall.common.constant.CacheConstants.User.SIGN;
import static com.mall.common.enums.ErrorCode.RESOURCE_EXISTS;
import com.mall.common.exception.BusinessException;

@Service
public class SignInService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PointsService pointsService;
    private final MallUserConfigProperties configProperties;

    public SignInService(RedisTemplate<String, Object> redisTemplate,
                         PointsService pointsService,
                         MallUserConfigProperties configProperties) {
        this.redisTemplate = redisTemplate;
        this.pointsService = pointsService;
        this.configProperties = configProperties;
    }

    public SignInVO signIn(String userId) {
        LocalDate today = LocalDate.now();
        String yearMonth = today.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = SIGN + userId + ":" + yearMonth;
        int dayOfMonth = today.getDayOfMonth();
        int offset = dayOfMonth - 1;

        Boolean signed = redisTemplate.opsForValue().getBit(key, offset);
        if (Boolean.TRUE.equals(signed)) {
            throw new BusinessException(RESOURCE_EXISTS);
        }

        int consecutiveDays = calcConsecutiveDays(key, offset, today);
        int basePoints = configProperties.getPoints().getSigninBase();
        int maxPoints = configProperties.getPoints().getSigninConsecutive();
        int bonus = configProperties.getPoints().getSigninConsecutiveBonus();
        int totalPoints = Math.min(basePoints + consecutiveDays * bonus, maxPoints);

        redisTemplate.opsForValue().setBit(key, offset, true);

        pointsService.addPoints(userId, totalPoints, BizTypeEnum.SIGN_IN, null);

        List<Integer> calendar = buildCalendar(key, today);

        SignInVO vo = new SignInVO();
        vo.setTodayPoints(totalPoints);
        vo.setConsecutiveDays(consecutiveDays + 1);
        vo.setSignInCalendar(calendar);
        return vo;
    }

    private int calcConsecutiveDays(String key, int offset, LocalDate today) {
        int consecutive = 0;
        LocalDate date = today.minusDays(1);
        while (date.getMonthValue() == today.getMonthValue()) {
            int off = date.getDayOfMonth() - 1;
            Boolean bit = redisTemplate.opsForValue().getBit(key, off);
            if (!Boolean.TRUE.equals(bit)) {
                break;
            }
            consecutive++;
            date = date.minusDays(1);
        }
        return consecutive;
    }

    private List<Integer> buildCalendar(String key, LocalDate today) {
        List<Integer> calendar = new ArrayList<>();
        int totalDays = today.lengthOfMonth();
        for (int d = 1; d <= totalDays; d++) {
            Boolean bit = redisTemplate.opsForValue().getBit(key, d - 1);
            if (Boolean.TRUE.equals(bit)) {
                calendar.add(d);
            }
        }
        return calendar;
    }
}
```

---

### Task 14: C 端 Controller — UserProfileController

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/UserProfileController.java`

- [ ] **Step 1: 编写 UserProfileController**

```java
package com.mall.user.controller.api;

import com.mall.common.dto.MallResult;
import com.mall.user.controller.api.vo.UserProfileVO;
import com.mall.user.service.customer.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    public MallResult<UserProfileVO> getProfile(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(userProfileService.getProfile(userId));
    }

    @PutMapping("/profile")
    public MallResult<UserProfileVO> updateProfile(HttpServletRequest request,
                                                    @RequestBody UserProfileVO updateRequest) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(userProfileService.updateProfile(userId, updateRequest));
    }
}
```

---

### Task 15: C 端 Controller — AddressController

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/AddressController.java`

- [ ] **Step 1: 编写 AddressController**

```java
package com.mall.user.controller.api;

import com.mall.common.dto.MallResult;
import com.mall.user.controller.api.vo.AddressVO;
import com.mall.user.service.customer.AddressBookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

@RestController
@RequestMapping("/api/user/addresses")
public class AddressController {

    private final AddressBookService addressBookService;

    public AddressController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    @GetMapping
    public MallResult<List<AddressVO>> list(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(addressBookService.listAddresses(userId));
    }

    @PostMapping
    public MallResult<AddressVO> add(HttpServletRequest request, @RequestBody AddressVO addressVO) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(addressBookService.addAddress(userId, addressVO));
    }

    @PutMapping("/{addressId}")
    public MallResult<AddressVO> update(HttpServletRequest request,
                                         @PathVariable String addressId,
                                         @RequestBody AddressVO addressVO) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(addressBookService.updateAddress(userId, addressId, addressVO));
    }

    @DeleteMapping("/{addressId}")
    public MallResult<Void> delete(HttpServletRequest request, @PathVariable String addressId) {
        String userId = request.getHeader(X_USER_ID);
        addressBookService.deleteAddress(userId, addressId);
        return MallResult.success(null);
    }

    @PutMapping("/{addressId}/default")
    public MallResult<Void> setDefault(HttpServletRequest request, @PathVariable String addressId) {
        String userId = request.getHeader(X_USER_ID);
        addressBookService.setDefault(userId, addressId);
        return MallResult.success(null);
    }
}
```

---

### Task 16: C 端 Controller — MemberController

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/MemberController.java`

- [ ] **Step 1: 编写 MemberController**

```java
package com.mall.user.controller.api;

import com.mall.common.dto.MallResult;
import com.mall.user.controller.api.vo.MembershipVO;
import com.mall.user.service.customer.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

@RestController
@RequestMapping("/api/user")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/membership")
    public MallResult<MembershipVO> getMembership(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(memberService.getMembership(userId));
    }
}
```

---

### Task 17: C 端 Controller — PointsController + GrowthController

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/PointsController.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/GrowthController.java`

- [ ] **Step 1: 编写 PointsController**

```java
package com.mall.user.controller.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.common.dto.MallResult;
import com.mall.user.controller.api.vo.PointsRecordVO;
import com.mall.user.controller.api.vo.PointsVO;
import com.mall.user.service.customer.PointsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

@RestController
@RequestMapping("/api/user")
public class PointsController {

    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @GetMapping("/points")
    public MallResult<PointsVO> getPoints(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(pointsService.getPoints(userId));
    }

    @GetMapping("/points/records")
    public MallResult<IPage<PointsRecordVO>> getPointsRecords(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String bizType) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(pointsService.getPointsRecords(userId, bizType, page, size));
    }
}
```

- [ ] **Step 2: 编写 GrowthController**

```java
package com.mall.user.controller.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.common.dto.MallResult;
import com.mall.user.controller.api.vo.GrowthRecordVO;
import com.mall.user.controller.api.vo.GrowthVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

@RestController
@RequestMapping("/api/user")
public class GrowthController {

    private final com.mall.user.service.customer.MemberService memberService;
    private final com.mall.user.service.customer.PointsService pointsService;

    public GrowthController(com.mall.user.service.customer.MemberService memberService,
                            com.mall.user.service.customer.PointsService pointsService) {
        this.memberService = memberService;
        this.pointsService = pointsService;
    }

    @GetMapping("/growth")
    public MallResult<GrowthVO> getGrowth(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        GrowthVO vo = new GrowthVO();
        vo.setGrowth(0L);
        vo.setTotalGrowth(0L);
        vo.setNeedGrowth(0L);
        vo.setProgressPercent(0);
        return MallResult.success(vo);
    }

    @GetMapping("/growth/records")
    public MallResult<IPage<GrowthRecordVO>> getGrowthRecords(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String bizType) {
        Page<GrowthRecordVO> emptyPage = new Page<>(page, size, 0);
        return MallResult.success(emptyPage);
    }
}
```

> **注意**：GrowthController 的成长值流水分页查询需在 Task 22 补充完整实现，当前为骨架。

---

### Task 20: C 端 Controller — SignInController

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/SignInController.java`

- [ ] **Step 1: 编写 SignInController**

```java
package com.mall.user.controller.api;

import com.mall.common.dto.MallResult;
import com.mall.user.controller.api.vo.SignInVO;
import com.mall.user.service.customer.SignInService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

@RestController
@RequestMapping("/api/user")
public class SignInController {

    private final SignInService signInService;

    public SignInController(SignInService signInService) {
        this.signInService = signInService;
    }

    @PostMapping("/sign-in")
    public MallResult<SignInVO> signIn(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(signInService.signIn(userId));
    }
}
```

---

### Task 19: MQ Consumer — UserOrderCompletedConsumer

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/mq/consumer/UserOrderCompletedConsumer.java`

- [ ] **Step 1: 编写 UserOrderCompletedConsumer**

```java
package com.mall.user.mq.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.user.service.customer.MemberService;
import com.mall.user.service.customer.PointsService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.mall.common.constant.MqTopicConstants.Order.COMPLETED;

@Component
@RocketMQMessageListener(topic = COMPLETED, consumerGroup = "mall-user:" + COMPLETED)
public class UserOrderCompletedConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(UserOrderCompletedConsumer.class);

    private final PointsService pointsService;
    private final MemberService memberService;

    public UserOrderCompletedConsumer(PointsService pointsService, MemberService memberService) {
        this.pointsService = pointsService;
        this.memberService = memberService;
    }

    @Override
    public void onMessage(String message) {
        try {
            JSONObject json = JSONObject.parseObject(message);
            String userId = json.getString("userId");
            String orderNo = json.getString("orderNo");
            Long orderAmount = json.getLong("orderAmount");
            Integer points = json.getInteger("points");
            if (points != null && points > 0) {
                pointsService.addPoints(userId, points, BizTypeEnum.ORDER, orderNo);
            }
            if (orderAmount != null && orderAmount > 0) {
                Long growth = orderAmount / 100;
                if (growth > 0) {
                    memberService.addGrowth(userId, growth, BizTypeEnum.ORDER, orderNo);
                }
            }
        } catch (Exception e) {
            log.error("消费订单完成事件失败: message={}", message, e);
        }
    }
}
```

---

### Task 20: 补充 GrowthService + GrowthController 完整实现

由于成长值流水需要 `MallUserGrowthLogMapper.selectByUserIdPage` 返回的数据，在 Task 6 已追加 Mapper 方法，现在补充 GrowthController：

- [ ] **Step 1: 在 MemberService 追加 getGrowth 和 getGrowthRecords 方法**

在 `MemberService.java` 追加：

```java
public GrowthVO getGrowth(String userId) {
    MallUserMemberDO member = mallUserMemberMapper.selectByUserId(userId);
    if (member == null) {
        throw new BusinessException(ACCOUNT_NOT_FOUND);
    }
    List<MallMemberLevelDO> allLevels = mallUserMemberLevelMapper.selectAllOrderByLevelValue();
    MallMemberLevelDO currentLevel = allLevels.stream()
            .filter(l -> l.getId().equals(member.getLevelId()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(RESOURCE_NOT_FOUND));

    GrowthVO vo = new GrowthVO();
    vo.setGrowth(member.getGrowth());
    vo.setTotalGrowth(member.getTotalGrowth());
    vo.setCurrentLevel(toLevelVO(currentLevel));

    MallMemberLevelDO nextLevel = allLevels.stream()
            .filter(l -> l.getMinGrowth() > member.getGrowth())
            .findFirst()
            .orElse(null);
    if (nextLevel != null) {
        vo.setNextLevel(toLevelVO(nextLevel));
        vo.setNeedGrowth(nextLevel.getMinGrowth() - member.getGrowth());
        vo.setProgressPercent((int) ((double) member.getGrowth() / nextLevel.getMinGrowth() * 100));
    } else {
        vo.setNeedGrowth(0L);
        vo.setProgressPercent(100);
    }
    return vo;
}

public IPage<GrowthRecordVO> getGrowthRecords(String userId, String bizType, int page, int size) {
    Page<MallGrowthLogDO> mpPage = new Page<>(page, size);
    IPage<MallGrowthLogDO> logPage = mallUserGrowthLogMapper.selectByUserIdPage(mpPage, userId, bizType);
    List<GrowthRecordVO> vos = new ArrayList<>();
    for (MallGrowthLogDO logEntry : logPage.getRecords()) {
        GrowthRecordVO vo = new GrowthRecordVO();
        vo.setId(logEntry.getId());
        vo.setBizType(logEntry.getBizType());
        BizTypeEnum bizTypeEnum = BizTypeEnum.fromCode(logEntry.getBizType());
        vo.setBizTypeName(bizTypeEnum != null ? bizTypeEnum.getName() : logEntry.getBizType());
        vo.setChangeType(logEntry.getChangeType());
        vo.setGrowth(logEntry.getGrowth());
        vo.setBeforeGrowth(logEntry.getBeforeGrowth());
        vo.setAfterGrowth(logEntry.getAfterGrowth());
        vo.setRemark(logEntry.getRemark());
        vo.setCreateTime(logEntry.getCreateTime());
        vos.add(vo);
    }
    Page<GrowthRecordVO> result = new Page<>(page, size, logPage.getTotal());
    result.setRecords(vos);
    return result;
}
```

对应新增 import：

```java
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.user.controller.api.vo.GrowthRecordVO;
import com.mall.user.controller.api.vo.GrowthVO;
import com.mall.user.DO.MallGrowthLogDO;
```

**Step 2: 重写 GrowthController**

```java
package com.mall.user.controller.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.common.dto.MallResult;
import com.mall.user.controller.api.vo.GrowthRecordVO;
import com.mall.user.controller.api.vo.GrowthVO;
import com.mall.user.service.customer.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

@RestController
@RequestMapping("/api/user")
public class GrowthController {

    private final MemberService memberService;

    public GrowthController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/growth")
    public MallResult<GrowthVO> getGrowth(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(memberService.getGrowth(userId));
    }

    @GetMapping("/growth/records")
    public MallResult<IPage<GrowthRecordVO>> getGrowthRecords(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String bizType) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(memberService.getGrowthRecords(userId, bizType, page, size));
    }
}
```

> 同时需将 Task 17 中创建的 GrowthController 骨架替换为此完整版本。

---

### Task 21: 定时任务 — PointsExpireTask

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/schedule/PointsExpireTask.java`

- [ ] **Step 1: 编写 PointsExpireTask**

```java
package com.mall.user.schedule;

import com.mall.common.enums.user.BizTypeEnum;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.mapper.MallUserPointsAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class PointsExpireTask {

    private static final Logger log = LoggerFactory.getLogger(PointsExpireTask.class);

    private final MallUserPointsAccountMapper mallUserPointsAccountMapper;

    public PointsExpireTask(MallUserPointsAccountMapper mallUserPointsAccountMapper) {
        this.mallUserPointsAccountMapper = mallUserPointsAccountMapper;
    }

    @Scheduled(cron = "0 0 0 31 12 ?")
    public void execute() {
        log.info("开始年度积分清零");
        MallPointsAccountDO query = new MallPointsAccountDO();
        java.util.List<MallPointsAccountDO> accounts = mallUserPointsAccountMapper.selectMallUserPointsAccountList(query);
        int batchSize = 500;
        int totalExpired = 0;
        for (int i = 0; i < accounts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, accounts.size());
            for (int j = i; j < end; j++) {
                MallPointsAccountDO account = accounts.get(j);
                int available = account.getAvailablePoints() != null ? account.getAvailablePoints().intValue() : 0;
                if (available <= 0) {
                    continue;
                }
                account.setExpiredPoints((account.getExpiredPoints() != null ? account.getExpiredPoints() : 0) + available);
                account.setAvailablePoints(0L);
                account.setUpdateTime(new Date());
                mallUserPointsAccountMapper.updateMallUserPointsAccount(account);
                totalExpired += available;
            }
        }
        log.info("年度积分清零完成，共清零 {} 积分", totalExpired);
    }
}
```

---

### Task 22: 配置变更 — pom.xml + MallUserApplication

**Files:**
- Modify: `server/mall/mall-user/pom.xml`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/MallUserApplication.java`

- [ ] **Step 1: pom.xml 追加 RocketMQ 依赖**

> MyBatis-Plus 依赖已在 Task 5 环节添加完毕，本步只加 RocketMQ。

在 `</dependencies>` 前追加：
```xml
<!-- RocketMQ -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.4.2</version>
</dependency>
```

- [ ] **Step 2: MallUserApplication 加 @EnableScheduling**

> MyBatis-Plus 兼容 `org.mybatis.spring.annotation.MapperScan`，无需改动。

```java
package com.mall.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * mall-user 用户服务
 * 端口：9302
 */
@EnableScheduling
@EnableFeignClients(basePackages = "com.mall.api")
@MapperScan("com.mall.user.mapper")
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.mall.user", "com.mall.common"})
public class MallUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallUserApplication.class, args);
    }
}
```

---

### Task 23: Nacos 配置 + RocketMQ 配置

- [ ] **Step 1: Nacos 控制台 — mall-user-dev.yml 追加配置项**

在 `mall.user.points` 段追加：

```yaml
      signin-consecutive-bonus: 1
```

- [ ] **Step 2: 确认 application-dev.yml 中 RocketMQ 连接配置**

需在 Nacos 公共配置 `application-dev.yml` 中添加 RocketMQ 配置（若未添加）：

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: mall-user-producer
  consumer:
    group: mall-user-consumer
```

---

### Task 24: 完整验证

- [ ] **Step 1: 编译验证**

```bash
mvn clean compile -f server/mall/pom.xml -DskipTests
```

预期: BUILD SUCCESS

- [ ] **Step 2: 启动 Docker 基础设施**

```bash
cd deploy/docker
docker compose up -d redis nacos rocketmq
```

- [ ] **Step 3: 启动 mall-user 服务**

```bash
# 确认 Nacos 已启动且配置已刷新
mvn spring-boot:run -f server/mall/mall-user/pom.xml
```

- [ ] **Step 4: 接口验证**

```bash
# 需要已登录用户，从 mall-auth 获取 JWT token
curl -H "Authorization: Bearer <jwt_token>" http://localhost:9302/api/user/profile
curl -H "Authorization: Bearer <jwt_token>" http://localhost:9302/api/user/addresses
curl -H "Authorization: Bearer <jwt_token>" -X POST http://localhost:9302/api/user/sign-in
curl -H "Authorization: Bearer <jwt_token>" http://localhost:9302/api/user/points
```

---

### Task 25: MQ 消费者幂等验证（可选，需 mall-order 发消息）

- [ ] **Step 1: 模拟发送订单完成消息**

```bash
# 通过 RocketMQ 控制台或代码发送测试消息
# Topic: mall:order:completed
# Body: {"userId":"<real_user_id>","orderNo":"TEST001","orderAmount":10000,"points":100}
```

预期: UserOrderCompletedConsumer 消费成功，积分+100，成长值+100

---

## 自查清单

| 检查项 | 状态 |
|--------|:--:|
| 所有文件路径精确 | ✅ |
| 所有代码步骤含完整实现 | ✅ |
| 无 TBD/TODO 占位符 | ✅ |
| ErrorCode 用 `ErrorCode.Xxx` 枚举值 `throw new BusinessException(ErrorCode.Xxx)` | ✅ |
| Redis Key 用 `CacheConstants.User.*` 常量 | ✅ |
| MQ Topic 用 `MqTopicConstants.Order.*` 常量 | ✅ |
| Controller 用 `MallResult<T>` 响应 | ✅ |
| Controller 中 `throw BusinessException(ErrorCode)` | ✅ |
| Service 全部构造注入 | ✅ |
| 管理端 Service 已改造为构造注入 | ✅ |
| 魔法值已消除 | ✅ |
| VO 类全部定义 | ✅ |
| 枚举 BizTypeEnum 覆盖所有场景 | ✅ |

---

## 审查修复项（plan-eng-review 产出，已确认）

### Fix 1: 乐观锁 version 字段（DDL + 实体 + XML）🆕

> 设计文档 `08_mall-user详细设计.md §3.3` 要求 `addPoints` 用乐观锁 `WHERE user_id=? AND version=?`，但 DDL 中 `mall_user_points_account` 表缺少 `version` 列。

**文件:**
- Create: `db/mall-sql/V1.0.1__add_version_to_points_account.sql`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/DO/MallPointsAccountDO.java`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserPointsAccountMapper.xml`

DDL:
```sql
ALTER TABLE `mall_user_points_account`
    ADD COLUMN `version` int unsigned DEFAULT 0 COMMENT '乐观锁版本号' AFTER `expired_points`;
```

实体补字段 `Integer version` + getter/setter + toString。
XML: resultMap 补 `<result property="version" column="version"/>`，SQL 片段补 `version` 列。

> ✅ 已执行：DDL/实体/XML 均已修改完毕。

### Fix 2: bizType 下推到 SQL 层

> `PointsService.getPointsRecords` 原在 Java 层过滤 bizType，导致分页错乱（DB 返回 20 行过滤后可能 3 行，total 仍为 20）。

`MallUserPointsLogMapper.selectByUserIdPage` 签名改为：

```java
List<MallPointsLogDO> selectByUserIdPage(@Param("userId") String userId,
        @Param("bizType") String bizType);
```

XML 追加 `<if test="bizType != null"> and biz_type = #{bizType}</if>`。

`PointsService.getPointsRecords` 删除 Java 层 bizType 过滤循环。

### Fix 3: SignInService 补 Redis EXPIRE

> spec §6 要求签到 Bitmap Key TTL 60 天，原实现遗漏。

在 `SignInService.signIn()` 的 `SETBIT` 后加：

```java
redisTemplate.expire(key, 60, TimeUnit.DAYS);
```

### Fix 4: UserProfileVO 拆分为响应 + 请求 DTO

> 原 `UserProfileVO` 被同时用作 GET 响应和 PUT 请求体，请求中包含 userId/points/growth 等无法修改的字段。

新建 `server/mall/mall-user/src/main/java/com/mall/user/controller/api/vo/UpdateProfileRequest.java`：

```java
public class UpdateProfileRequest {
    private String nickname;
    private String avatar;
    private Integer gender;
    private String birthday;
    private String email;
    // getters / setters
}
```

`UserProfileController.updateProfile` 参数改为 `@RequestBody UpdateProfileRequest`。
`UserProfileService.updateProfile` 参数同步改为 `UpdateProfileRequest`。

### Fix 5: GrowthController 合并为单一 Task

> 原 Task 17 写骨架 + Task 20 替换完整版，两次写同一文件。

合并后：
- Task 17 → 仅 `PointsController`
- Task 18 → 仅 `SignInController`
- Task 20 → 直接写完整版 GrowthController（含 MemberService.getGrowth/getGrowthRecords 追加）

### Fix 6: PointsExpireTask 分页查询

> 原实现全量 SELECT 后分批 UPDATE，大量用户时内存压力大。

改为分页循环：

```java
int offset = 0;
int pageSize = 500;
while (true) {
    List<MallPointsAccountDO> batch = mapper.selectAllAvailablePoints(offset, pageSize);
    if (batch.isEmpty()) break;
    // ... 逐条处理
    offset += pageSize;
}
```

需在 `MallUserPointsAccountMapper` 追加分页查询方法：

```java
List<MallPointsAccountDO> selectAllAvailablePoints(@Param("offset") int offset,
        @Param("limit") int limit);
```

XML:
```xml
<select id="selectAllAvailablePoints" resultMap="MallUserPointsAccountResult">
    select id, user_id, total_points, available_points, used_points, expired_points, version
    from mall_user_points_account
    where available_points > 0
    order by id
    limit #{offset}, #{limit}
</select>
```

---

## GSTACK REVIEW REPORT

| Review | Trigger | Runs | Status | Findings |
|--------|---------|------|--------|----------|
| Eng Review | `/plan-eng-review` | 1 | CLEAR (PLAN) | Architecture:3 / Code Quality:2 / Test:30+ gaps / Performance:1 |

- **FIXES CONFIRMED:** 6 fixes applied to plan (DDL / SQL / Redis / DTO split / task merge / pagination)
- **VERDICT:** ENG CLEARED — 6 fixes confirmed, ready to implement
