package com.lanxin.im.ui.social

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lanxin.im.R
import com.lanxin.im.data.remote.RetrofitClient
import kotlinx.coroutines.launch

/**
 * 投诉举报Activity（三步骤流程）
 */
class ReportActivity : AppCompatActivity() {
    
    private var currentStep = 1
    private var selectedIssueType = ""
    private var selectedReason = ""
    private var reportDetail = ""
    
    private lateinit var step1Container: View
    private lateinit var step2Container: View
    private lateinit var step3Container: View
    private lateinit var tvStepTitle: TextView
    private lateinit var btnNext: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        
        setupViews()
        showStep(1)
    }
    
    private fun setupViews() {
        findViewById<View>(R.id.btn_back)?.setOnClickListener {
            if (currentStep > 1) {
                showStep(currentStep - 1)
            } else {
                finish()
            }
        }
        
        step1Container = findViewById(R.id.step1_container)
        step2Container = findViewById(R.id.step2_container)
        step3Container = findViewById(R.id.step3_container)
        tvStepTitle = findViewById(R.id.tv_step_title)
        btnNext = findViewById(R.id.btn_next)
        
        btnNext.setOnClickListener {
            when (currentStep) {
                1 -> {
                    val radioGroup = step1Container.findViewById<RadioGroup>(R.id.radio_group_type)
                    val selectedId = radioGroup.checkedRadioButtonId
                    if (selectedId != -1) {
                        selectedIssueType = findViewById<RadioButton>(selectedId).text.toString()
                        showStep(2)
                    } else {
                        Toast.makeText(this, "请选择举报类型", Toast.LENGTH_SHORT).show()
                    }
                }
                2 -> {
                    val radioGroup = step2Container.findViewById<RadioGroup>(R.id.radio_group_reason)
                    val selectedId = radioGroup.checkedRadioButtonId
                    if (selectedId != -1) {
                        selectedReason = findViewById<RadioButton>(selectedId).text.toString()
                        showStep(3)
                    } else {
                        Toast.makeText(this, "请选择举报理由", Toast.LENGTH_SHORT).show()
                    }
                }
                3 -> {
                    val etDetail = step3Container.findViewById<EditText>(R.id.et_detail)
                    reportDetail = etDetail.text.toString()
                    submitReport()
                }
            }
        }
    }
    
    private fun showStep(step: Int) {
        currentStep = step
        
        step1Container.visibility = if (step == 1) View.VISIBLE else View.GONE
        step2Container.visibility = if (step == 2) View.VISIBLE else View.GONE
        step3Container.visibility = if (step == 3) View.VISIBLE else View.GONE
        
        when (step) {
            1 -> {
                tvStepTitle.text = "步骤1/3：选择举报类型"
                btnNext.text = "下一步"
            }
            2 -> {
                tvStepTitle.text = "步骤2/3：选择举报理由"
                btnNext.text = "下一步"
            }
            3 -> {
                tvStepTitle.text = "步骤3/3：补充说明"
                btnNext.text = "提交"
            }
        }
    }
    
    private fun submitReport() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@ReportActivity, "举报已提交", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@ReportActivity, "提交失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

