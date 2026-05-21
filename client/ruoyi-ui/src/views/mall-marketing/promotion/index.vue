<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="160px">
      <el-form-item label="活动名称" prop="promotionName">
        <el-input
          v-model="queryParams.promotionName"
          placeholder="请输入活动名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="活动开始时间" prop="startTime">
        <el-date-picker clearable
          v-model="queryParams.startTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择活动开始时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="活动结束时间" prop="endTime">
        <el-date-picker clearable
          v-model="queryParams.endTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择活动结束时间">
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
          v-hasPermi="['mall-marketing:promotion:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-marketing:promotion:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-marketing:promotion:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-marketing:promotion:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="promotionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="活动名称" align="center" prop="promotionName" />
      <el-table-column label="活动类型" align="center" prop="promotionType" />
      <el-table-column label="活动开始时间" align="center" prop="startTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="活动结束时间" align="center" prop="endTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="活动状态" align="center" prop="promotionStatus" />
      <el-table-column label="活动描述" align="center" prop="description" />
      <el-table-column label="活动 Banner 图 URL" align="center" prop="bannerImage" width="100">
        <template #default="scope">
          <image-preview :src="scope.row.bannerImage" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column label="排序值" align="center" prop="sortOrder" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-marketing:promotion:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-marketing:promotion:remove']">删除</el-button>
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

    <!-- 添加或修改活动管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="promotionRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="活动名称" prop="promotionName">
              <el-input v-model="form.promotionName" placeholder="请输入活动名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="活动开始时间" prop="startTime">
              <el-date-picker clearable
                v-model="form.startTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择活动开始时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="活动结束时间" prop="endTime">
              <el-date-picker clearable
                v-model="form.endTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择活动结束时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="活动描述" prop="description">
              <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="活动 Banner 图 URL" prop="bannerImage">
              <image-upload v-model="form.bannerImage"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="center">促销规则信息</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="Plus" @click="handleAddMallMarketingPromotionRule">添加</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" icon="Delete" @click="handleDeleteMallMarketingPromotionRule">删除</el-button>
          </el-col>
        </el-row>
        <el-table :data="mallMarketingPromotionRuleList" @selection-change="handleMallMarketingPromotionRuleSelectionChange" ref="mallMarketingPromotionRule">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="序号" width="60">
            <template #default="{ $index }">
              {{ $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="规则类型" prop="ruleType" width="150">
            <template #default="scope">
              <el-select v-model="scope.row.ruleType" placeholder="请选择规则类型">
                <el-option label="请选择字典生成" value="" />
              </el-select>
            </template>
          </el-table-column>
        </el-table>
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

<script setup lang="ts" name="Promotion">
import type { MallMarketingPromotion, MallMarketingPromotionRule, PromotionQueryParams } from "@/types/api/mall-marketing/promotion"
import { listPromotion, getPromotion, delPromotion, addPromotion, updatePromotion } from "@/api/mall-marketing/promotion"

const { proxy } = getCurrentInstance()

const promotionList = ref<MallMarketingPromotion[]>([])
const mallMarketingPromotionRuleList = ref([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const checkedMallMarketingPromotionRule = ref([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallMarketingPromotion,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    promotionName: undefined,
    promotionType: undefined,
    startTime: undefined,
    endTime: undefined,
    promotionStatus: undefined,
    description: undefined,
    bannerImage: undefined,
    sortOrder: undefined
  } as PromotionQueryParams,
  rules: {
    promotionName: [
      { required: true, message: "活动名称不能为空", trigger: "blur" }
    ],
    promotionType: [
      { required: true, message: "活动类型不能为空", trigger: "change" }
    ],
    startTime: [
      { required: true, message: "活动开始时间不能为空", trigger: "blur" }
    ],
    endTime: [
      { required: true, message: "活动结束时间不能为空", trigger: "blur" }
    ],
    promotionStatus: [
      { required: true, message: "活动状态不能为空", trigger: "change" }
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

/** 查询活动管理列表 */
function getList() {
  loading.value = true
  listPromotion(queryParams.value).then(response => {
    promotionList.value = response.rows
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
    promotionName: null,
    promotionType: null,
    startTime: null,
    endTime: null,
    promotionStatus: null,
    description: null,
    bannerImage: null,
    sortOrder: null,
    createBy: null,
    updateBy: null,
    createTime: null,
    updateTime: null
  }
  mallMarketingPromotionRuleList.value = []
  proxy.resetForm("promotionRef")
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
function handleSelectionChange(selection: MallMarketingPromotion[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加活动管理"
}

/** 修改按钮操作 */
function handleUpdate(row: MallMarketingPromotion) {
  reset()
  const _id = row.id || ids.value[0]
  getPromotion(_id).then(response => {
    form.value = response.data
    mallMarketingPromotionRuleList.value = response.data?.mallMarketingPromotionRuleList ?? []
    open.value = true
    title.value = "修改活动管理"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["promotionRef"].validate((valid: boolean) => {
    if (valid) {
      form.value.mallMarketingPromotionRuleList = mallMarketingPromotionRuleList.value
      if (form.value.id != null) {
        updatePromotion(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addPromotion(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallMarketingPromotion) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除活动管理编号为"' + _ids + '"的数据项？').then(function() {
    return delPromotion(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 促销规则添加按钮操作 */
function handleAddMallMarketingPromotionRule() {
  let obj: MallMarketingPromotionRule = {}
  obj.ruleType = undefined
  obj.thresholdAmount = undefined
  obj.benefitAmount = undefined
  obj.benefitRate = undefined
  obj.isExclusive = undefined
  obj.sortOrder = undefined
  mallMarketingPromotionRuleList.value.push(obj)
}

/** 促销规则删除按钮操作 */
function handleDeleteMallMarketingPromotionRule() {
  if (checkedMallMarketingPromotionRule.value.length == 0) {
    proxy.$modal.msgError("请先选择要删除的促销规则数据")
  } else {
    const mallMarketingPromotionRules = mallMarketingPromotionRuleList.value
    const checkedMallMarketingPromotionRules = checkedMallMarketingPromotionRule.value
    mallMarketingPromotionRuleList.value = mallMarketingPromotionRules.filter(function(item: any) {
      return checkedMallMarketingPromotionRules.indexOf(item.index) == -1
    })
  }
}

/** 复选框选中数据 */
function handleMallMarketingPromotionRuleSelectionChange(selection: any[]) {
  checkedMallMarketingPromotionRule.value = selection.map(item => item.index)
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-marketing/promotion/export', {
    ...queryParams.value
  }, `promotion_${new Date().getTime()}.xlsx`)
}

getList()
</script>
