<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="160px">
      <el-form-item label="最近一次等级生效时间" prop="levelStartTime">
        <el-date-picker clearable
          v-model="queryParams.levelStartTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择最近一次等级生效时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="等级到期时间" prop="levelEndTime">
        <el-date-picker clearable
          v-model="queryParams.levelEndTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择等级到期时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="首次成为会员时间" prop="becomeTime">
        <el-date-picker clearable
          v-model="queryParams.becomeTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择首次成为会员时间">
        </el-date-picker>
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
          v-hasPermi="['mall-admin:member:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-admin:member:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-admin:member:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-admin:member:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="memberList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="用户 ID" align="center" prop="userId" />
      <el-table-column label="当前等级" align="center" prop="levelId" />
      <el-table-column label="当前成长值" align="center" prop="growth" />
      <el-table-column label="累计成长值" align="center" prop="totalGrowth" />
      <el-table-column label="最近一次等级生效时间" align="center" prop="levelStartTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.levelStartTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="等级到期时间" align="center" prop="levelEndTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.levelEndTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="首次成为会员时间" align="center" prop="becomeTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.becomeTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="逻辑删除标志" align="center" prop="isDeleted" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-admin:member:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-admin:member:remove']">删除</el-button>
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

    <!-- 添加或修改用户会员信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="memberRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="最近一次等级生效时间" prop="levelStartTime">
              <el-date-picker clearable
                v-model="form.levelStartTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择最近一次等级生效时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="等级到期时间" prop="levelEndTime">
              <el-date-picker clearable
                v-model="form.levelEndTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择等级到期时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="首次成为会员时间" prop="becomeTime">
              <el-date-picker clearable
                v-model="form.becomeTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择首次成为会员时间">
              </el-date-picker>
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

<script setup lang="ts" name="Member">
import type { MallUserMember, MemberQueryParams } from "@/types/api/mall-user/member"
import { listMember, getMember, delMember, addMember, updateMember } from "@/api/mall-user/member"

const { proxy } = getCurrentInstance()

const memberList = ref<MallUserMember[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallUserMember,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined,
    levelId: undefined,
    growth: undefined,
    totalGrowth: undefined,
    levelStartTime: undefined,
    levelEndTime: undefined,
    becomeTime: undefined,
    isDeleted: undefined,
  } as MemberQueryParams,
  rules: {
    userId: [
      { required: true, message: "用户 ID不能为空", trigger: "blur" }
    ],
    createTime: [
      { required: true, message: "创建时间不能为空", trigger: "blur" }
    ],
    updateTime: [
      { required: true, message: "修改时间不能为空", trigger: "blur" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询用户会员信息列表 */
function getList() {
  loading.value = true
  listMember(queryParams.value).then(response => {
    memberList.value = response.rows
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
    userId: null,
    levelId: null,
    growth: null,
    totalGrowth: null,
    levelStartTime: null,
    levelEndTime: null,
    becomeTime: null,
    isDeleted: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("memberRef")
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
function handleSelectionChange(selection: MallUserMember[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加用户会员信息"
}

/** 修改按钮操作 */
function handleUpdate(row: MallUserMember) {
  reset()
  const _id = row.id || ids.value[0]
  getMember(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改用户会员信息"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["memberRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateMember(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addMember(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallUserMember) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除用户会员信息编号为"' + _ids + '"的数据项？').then(function() {
    return delMember(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-admin/member/export', {
    ...queryParams.value
  }, `member_${new Date().getTime()}.xlsx`)
}

getList()
</script>
