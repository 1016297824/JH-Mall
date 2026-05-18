<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入手机号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号 SHA256 哈希辅助列，用于等值查询" prop="phoneHash">
        <el-input
          v-model="queryParams.phoneHash"
          placeholder="请输入手机号 SHA256 哈希辅助列，用于等值查询"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="密码 BCrypt 哈希" prop="password">
        <el-input
          v-model="queryParams.password"
          placeholder="请输入密码 BCrypt 哈希"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input
          v-model="queryParams.nickname"
          placeholder="请输入昵称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input
          v-model="queryParams.email"
          placeholder="请输入邮箱"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="邮箱 SHA256 哈希辅助列" prop="emailHash">
        <el-input
          v-model="queryParams.emailHash"
          placeholder="请输入邮箱 SHA256 哈希辅助列"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生日" prop="birthday">
        <el-date-picker clearable
          v-model="queryParams.birthday"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择生日">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="注册 IP" prop="registerIp">
        <el-input
          v-model="queryParams.registerIp"
          placeholder="请输入注册 IP"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="注册时间" prop="registerTime">
        <el-date-picker clearable
          v-model="queryParams.registerTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择注册时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="最后登录时间" prop="lastLoginTime">
        <el-date-picker clearable
          v-model="queryParams.lastLoginTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择最后登录时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="最后登录 IP" prop="lastLoginIp">
        <el-input
          v-model="queryParams.lastLoginIp"
          placeholder="请输入最后登录 IP"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="同意隐私协议时间" prop="privacyAgreedTime">
        <el-date-picker clearable
          v-model="queryParams.privacyAgreedTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择同意隐私协议时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="微信 OpenID" prop="wechatOpenid">
        <el-input
          v-model="queryParams.wechatOpenid"
          placeholder="请输入微信 OpenID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="微信 UnionID" prop="wechatUnionid">
        <el-input
          v-model="queryParams.wechatUnionid"
          placeholder="请输入微信 UnionID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['mall-user:user:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-user:user:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-user:user:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-user:user:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="手机号" align="center" prop="phone" />
      <el-table-column label="手机号 SHA256 哈希辅助列，用于等值查询" align="center" prop="phoneHash" />
      <el-table-column label="密码 BCrypt 哈希" align="center" prop="password" />
      <el-table-column label="昵称" align="center" prop="nickname" />
      <el-table-column label="头像 URL" align="center" prop="avatar" />
      <el-table-column label="邮箱" align="center" prop="email" />
      <el-table-column label="邮箱 SHA256 哈希辅助列" align="center" prop="emailHash" />
      <el-table-column label="性别：0=未知 1=男 2=女" align="center" prop="gender" />
      <el-table-column label="生日" align="center" prop="birthday" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.birthday, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="用户状态：0=正常 1=冻结 2=注销" align="center" prop="userStatus" />
      <el-table-column label="注册方式：phone / wechat / email" align="center" prop="registerType" />
      <el-table-column label="注册 IP" align="center" prop="registerIp" />
      <el-table-column label="注册时间" align="center" prop="registerTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.registerTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最后登录时间" align="center" prop="lastLoginTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.lastLoginTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最后登录 IP" align="center" prop="lastLoginIp" />
      <el-table-column label="是否同意隐私协议：1=同意 0=未同意" align="center" prop="isPrivacyAgreed" />
      <el-table-column label="同意隐私协议时间" align="center" prop="privacyAgreedTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.privacyAgreedTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="微信 OpenID" align="center" prop="wechatOpenid" />
      <el-table-column label="微信 UnionID" align="center" prop="wechatUnionid" />
      <el-table-column label="逻辑删除标志：1=已删除 0=未删除" align="center" prop="isDeleted" />
      <el-table-column label="乐观锁版本号" align="center" prop="version" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-user:user:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-user:user:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改用户账号对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="userRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="手机号 SHA256 哈希辅助列，用于等值查询" prop="phoneHash">
              <el-input v-model="form.phoneHash" placeholder="请输入手机号 SHA256 哈希辅助列，用于等值查询" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="密码 BCrypt 哈希" prop="password">
              <el-input v-model="form.password" placeholder="请输入密码 BCrypt 哈希" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="头像 URL" prop="avatar">
              <el-input v-model="form.avatar" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮箱 SHA256 哈希辅助列" prop="emailHash">
              <el-input v-model="form.emailHash" placeholder="请输入邮箱 SHA256 哈希辅助列" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="生日" prop="birthday">
              <el-date-picker clearable
                v-model="form.birthday"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择生日">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="注册 IP" prop="registerIp">
              <el-input v-model="form.registerIp" placeholder="请输入注册 IP" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="注册时间" prop="registerTime">
              <el-date-picker clearable
                v-model="form.registerTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择注册时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="最后登录时间" prop="lastLoginTime">
              <el-date-picker clearable
                v-model="form.lastLoginTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择最后登录时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="最后登录 IP" prop="lastLoginIp">
              <el-input v-model="form.lastLoginIp" placeholder="请输入最后登录 IP" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="同意隐私协议时间" prop="privacyAgreedTime">
              <el-date-picker clearable
                v-model="form.privacyAgreedTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择同意隐私协议时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="微信 OpenID" prop="wechatOpenid">
              <el-input v-model="form.wechatOpenid" placeholder="请输入微信 OpenID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="微信 UnionID" prop="wechatUnionid">
              <el-input v-model="form.wechatUnionid" placeholder="请输入微信 UnionID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="User">
import type { MallUser, UserQueryParams } from "@/types/api/mall-user/user"
import { listUser, getUser, delUser, addUser, updateUser } from "@/api/mall-user/user"

const { proxy } = getCurrentInstance()

const userList = ref<MallUser[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallUser,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    phone: undefined,
    phoneHash: undefined,
    password: undefined,
    nickname: undefined,
    avatar: undefined,
    email: undefined,
    emailHash: undefined,
    gender: undefined,
    birthday: undefined,
    userStatus: undefined,
    registerType: undefined,
    registerIp: undefined,
    registerTime: undefined,
    lastLoginTime: undefined,
    lastLoginIp: undefined,
    isPrivacyAgreed: undefined,
    privacyAgreedTime: undefined,
    wechatOpenid: undefined,
    wechatUnionid: undefined,
    isDeleted: undefined,
    version: undefined,
  } as UserQueryParams,
  rules: {
    phone: [
      { required: true, message: "手机号不能为空", trigger: "blur" }
    ],
    phoneHash: [
      { required: true, message: "手机号 SHA256 哈希辅助列，用于等值查询不能为空", trigger: "blur" }
    ],
    password: [
      { required: true, message: "密码 BCrypt 哈希不能为空", trigger: "blur" }
    ],
    createTime: [
      { required: true, message: "创建时间不能为空", trigger: "blur" }
    ],
    updateTime: [
      { required: true, message: "修改时间不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询用户账号列表 */
function getList() {
  loading.value = true
  listUser(queryParams.value).then(response => {
    userList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: null,
    phone: null,
    phoneHash: null,
    password: null,
    nickname: null,
    avatar: null,
    email: null,
    emailHash: null,
    gender: null,
    birthday: null,
    userStatus: null,
    registerType: null,
    registerIp: null,
    registerTime: null,
    lastLoginTime: null,
    lastLoginIp: null,
    isPrivacyAgreed: null,
    privacyAgreedTime: null,
    wechatOpenid: null,
    wechatUnionid: null,
    isDeleted: null,
    createBy: null,
    updateBy: null,
    createTime: null,
    updateTime: null,
    version: null,
    remark: null
  }
  proxy.resetForm("userRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection: MallUser[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加用户账号"
}

/** 修改按钮操作 */
function handleUpdate(row: MallUser) {
  reset()
  const _id = row.id || ids.value[0]
  getUser(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改用户账号"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["userRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateUser(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addUser(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallUser) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除用户账号编号为"' + _ids + '"的数据项？').then(function() {
    return delUser(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-user/user/export', {
    ...queryParams.value
  }, `user_${new Date().getTime()}.xlsx`)
}

getList()
</script>
