<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="160px">
      <el-form-item label="业务单号" prop="bizNo">
        <el-input
          v-model="queryParams.bizNo"
          placeholder="请输入业务单号"
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
          v-hasPermi="['mall-admin:growth_log:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-admin:growth_log:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-admin:growth_log:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-admin:growth_log:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="growth_logList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="用户 ID" align="center" prop="userId" />
      <el-table-column label="业务类型" align="center" prop="bizType" />
      <el-table-column label="业务单号" align="center" prop="bizNo" />
      <el-table-column label="变动方向" align="center" prop="changeType" />
      <el-table-column label="本次变动成长值" align="center" prop="growth" />
      <el-table-column label="变动前成长值余额" align="center" prop="beforeGrowth" />
      <el-table-column label="变动后成长值余额" align="center" prop="afterGrowth" />
      <el-table-column label="变动原因说明" align="center" prop="remark" />
      <el-table-column label="逻辑删除标志" align="center" prop="isDeleted" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-admin:growth_log:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-admin:growth_log:remove']">删除</el-button>
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

    <!-- 添加或修改成长值流水对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="growth_logRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="业务单号" prop="bizNo">
              <el-input v-model="form.bizNo" placeholder="请输入业务单号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="变动原因说明" prop="remark">
              <el-input v-model="form.remark" placeholder="请输入变动原因说明" />
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

<script setup lang="ts" name="Growth_log">
import type { MallUserGrowthLog, Growth_logQueryParams } from "@/types/api/mall-user/growth_log"
import { listGrowth_log, getGrowth_log, delGrowth_log, addGrowth_log, updateGrowth_log } from "@/api/mall-user/growth_log"

const { proxy } = getCurrentInstance()

const growth_logList = ref<MallUserGrowthLog[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallUserGrowthLog,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined,
    bizType: undefined,
    bizNo: undefined,
    changeType: undefined,
    growth: undefined,
    beforeGrowth: undefined,
    afterGrowth: undefined,
    isDeleted: undefined,
  } as Growth_logQueryParams,
  rules: {
    userId: [
      { required: true, message: "用户 ID不能为空", trigger: "blur" }
    ],
    bizType: [
      { required: true, message: "业务类型不能为空", trigger: "change" }
    ],
    changeType: [
      { required: true, message: "变动方向不能为空", trigger: "change" }
    ],
    growth: [
      { required: true, message: "本次变动成长值不能为空", trigger: "blur" }
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

/** 查询成长值流水列表 */
function getList() {
  loading.value = true
  listGrowth_log(queryParams.value).then(response => {
    growth_logList.value = response.rows
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
    bizType: null,
    bizNo: null,
    changeType: null,
    growth: null,
    beforeGrowth: null,
    afterGrowth: null,
    remark: null,
    isDeleted: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("growth_logRef")
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
function handleSelectionChange(selection: MallUserGrowthLog[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加成长值流水"
}

/** 修改按钮操作 */
function handleUpdate(row: MallUserGrowthLog) {
  reset()
  const _id = row.id || ids.value[0]
  getGrowth_log(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改成长值流水"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["growth_logRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateGrowth_log(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addGrowth_log(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallUserGrowthLog) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除成长值流水编号为"' + _ids + '"的数据项？').then(function() {
    return delGrowth_log(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-admin/growth_log/export', {
    ...queryParams.value
  }, `growth_log_${new Date().getTime()}.xlsx`)
}

getList()
</script>
