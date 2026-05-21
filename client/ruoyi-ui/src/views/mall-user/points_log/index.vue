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
          v-hasPermi="['mall-user:points_log:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-user:points_log:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-user:points_log:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-user:points_log:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="points_logList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="用户 ID" align="center" prop="userId" />
      <el-table-column label="业务类型" align="center" prop="bizType" />
      <el-table-column label="业务单号" align="center" prop="bizNo" />
      <el-table-column label="变动方向" align="center" prop="changeType" />
      <el-table-column label="本次变动积分值" align="center" prop="points" />
      <el-table-column label="变动前积分余额" align="center" prop="beforePoints" />
      <el-table-column label="变动后积分余额" align="center" prop="afterPoints" />
      <el-table-column label="变动原因说明" align="center" prop="remark" />
      <el-table-column label="逻辑删除标志" align="center" prop="isDeleted" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-user:points_log:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-user:points_log:remove']">删除</el-button>
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

    <!-- 添加或修改积分流水对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="points_logRef" :model="form" :rules="rules" label-width="120px">
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

<script setup lang="ts" name="Points_log">
import type { MallUserPointsLog, Points_logQueryParams } from "@/types/api/mall-user/points_log"
import { listPoints_log, getPoints_log, delPoints_log, addPoints_log, updatePoints_log } from "@/api/mall-user/points_log"

const { proxy } = getCurrentInstance()

const points_logList = ref<MallUserPointsLog[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallUserPointsLog,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined,
    bizType: undefined,
    bizNo: undefined,
    changeType: undefined,
    points: undefined,
    beforePoints: undefined,
    afterPoints: undefined,
    isDeleted: undefined,
  } as Points_logQueryParams,
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
    points: [
      { required: true, message: "本次变动积分值不能为空", trigger: "blur" }
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

/** 查询积分流水列表 */
function getList() {
  loading.value = true
  listPoints_log(queryParams.value).then(response => {
    points_logList.value = response.rows
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
    points: null,
    beforePoints: null,
    afterPoints: null,
    remark: null,
    isDeleted: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("points_logRef")
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
function handleSelectionChange(selection: MallUserPointsLog[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加积分流水"
}

/** 修改按钮操作 */
function handleUpdate(row: MallUserPointsLog) {
  reset()
  const _id = row.id || ids.value[0]
  getPoints_log(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改积分流水"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["points_logRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updatePoints_log(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addPoints_log(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallUserPointsLog) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除积分流水编号为"' + _ids + '"的数据项？').then(function() {
    return delPoints_log(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-user/points_log/export', {
    ...queryParams.value
  }, `points_log_${new Date().getTime()}.xlsx`)
}

getList()
</script>
