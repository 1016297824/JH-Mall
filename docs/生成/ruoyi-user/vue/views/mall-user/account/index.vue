<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
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
          v-hasPermi="['mall-user:account:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-user:account:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-user:account:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-user:account:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="accountList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="用户 ID，与用户一对一" align="center" prop="userId" />
      <el-table-column label="累计获取积分，仅增不减" align="center" prop="totalPoints" />
      <el-table-column label="可用积分余额" align="center" prop="availablePoints" />
      <el-table-column label="已使用积分" align="center" prop="usedPoints" />
      <el-table-column label="已过期积分" align="center" prop="expiredPoints" />
      <el-table-column label="逻辑删除标志" align="center" prop="isDeleted" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-user:account:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-user:account:remove']">删除</el-button>
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

    <!-- 添加或修改积分账户对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="accountRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
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

<script setup lang="ts" name="Account">
import type { MallUserPointsAccount, AccountQueryParams } from "@/types/api/mall-user/account"
import { listAccount, getAccount, delAccount, addAccount, updateAccount } from "@/api/mall-user/account"

const { proxy } = getCurrentInstance()

const accountList = ref<MallUserPointsAccount[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallUserPointsAccount,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined,
    totalPoints: undefined,
    availablePoints: undefined,
    usedPoints: undefined,
    expiredPoints: undefined,
    isDeleted: undefined,
  } as AccountQueryParams,
  rules: {
    userId: [
      { required: true, message: "用户 ID，与用户一对一不能为空", trigger: "blur" }
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

/** 查询积分账户列表 */
function getList() {
  loading.value = true
  listAccount(queryParams.value).then(response => {
    accountList.value = response.rows
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
    totalPoints: null,
    availablePoints: null,
    usedPoints: null,
    expiredPoints: null,
    isDeleted: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("accountRef")
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
function handleSelectionChange(selection: MallUserPointsAccount[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加积分账户"
}

/** 修改按钮操作 */
function handleUpdate(row: MallUserPointsAccount) {
  reset()
  const _id = row.id || ids.value[0]
  getAccount(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改积分账户"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["accountRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateAccount(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAccount(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallUserPointsAccount) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除积分账户编号为"' + _ids + '"的数据项？').then(function() {
    return delAccount(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-user/account/export', {
    ...queryParams.value
  }, `account_${new Date().getTime()}.xlsx`)
}

getList()
</script>
