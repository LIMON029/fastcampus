package kr.co.limon.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.room.Room
import kr.co.limon.calculator.databinding.ActivityMainBinding
import kr.co.limon.calculator.model.History
import java.lang.NumberFormatException

class MainActivity : AppCompatActivity() {
    var _binding : ActivityMainBinding ?= null
    val binding get() = _binding!!

    private var isOperator = false
    private var hasOperator = false

    lateinit var db : AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "historyDB"
        ).build()
    }

    fun historyButtonClicked(view : View) {
        binding.historyLayout.isVisible = true
        binding.historyLinearLayout.removeAllViews()

        Thread(Runnable {
            db.historyDao().getAll().reversed().forEach {
                runOnUiThread {
                    val historyView = LayoutInflater.from(this).inflate(R.layout.history_row, null, false)
                    historyView.findViewById<TextView>(R.id.expressionTextView).text = it.expression
                    historyView.findViewById<TextView>(R.id.resultTextView).text = "= ${it.result}"

                    binding.historyLinearLayout.addView(historyView)
                }
            }
        }).start()
    }

    fun historyClearButtonClicked(view : View) {
        binding.historyLinearLayout.removeAllViews()
        Thread(Runnable {
            db.historyDao().deleteAll()
        }).start()
    }

    fun closedHistoryButtonClicked(view : View) {
        binding.historyLayout.isVisible = false
    }

    fun resultButtonClicked(view : View) {
        val expressionTexts = binding.expressionTextView.text.split(" ")
        if(binding.expressionTextView.text.isEmpty() || expressionTexts.size == 1){
            return
        }

        if(expressionTexts.size != 3 && hasOperator){
            Toast.makeText(this, "아직 완성되지 않은 수식입니다", Toast.LENGTH_SHORT).show()
            return
        }

        if(expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()){
            Toast.makeText(this, "오류 발생", Toast.LENGTH_SHORT).show()
            return
        }

        val expressionText = binding.expressionTextView.text.toString()
        val resultText = calculateExpression()

        // 디비에 넣기
        Thread(Runnable {
            db.historyDao().insertHistory(History(null, expressionText, resultText))
        }).start()


        binding.resultTextView.text = ""
        binding.expressionTextView.text = resultText

        isOperator = false
        hasOperator = false
    }

    fun clearButtonClicked(view : View) {
        binding.expressionTextView.text = ""
        binding.resultTextView.text = ""
        isOperator = false
        hasOperator = false
    }

    fun buttonClicked(view : View) {
        when(view.id) {
            R.id.keyZero -> numberButtonClicked("0")
            R.id.keyOne -> numberButtonClicked("1")
            R.id.keyTwo -> numberButtonClicked("2")
            R.id.keyThree -> numberButtonClicked("3")
            R.id.keyFour -> numberButtonClicked("4")
            R.id.keyFive -> numberButtonClicked("5")
            R.id.keySix -> numberButtonClicked("6")
            R.id.keySeven -> numberButtonClicked("7")
            R.id.keyEight -> numberButtonClicked("8")
            R.id.keyNine -> numberButtonClicked("9")
            R.id.keyPlus -> operatorButtonClicked("+")
            R.id.keyMinus -> operatorButtonClicked("-")
            R.id.keyMulti -> operatorButtonClicked("*")
            R.id.keyDivider -> operatorButtonClicked("/")
            R.id.keyModular -> operatorButtonClicked("%")
        }
    }

    private fun operatorButtonClicked(operator : String){
        if(binding.expressionTextView.text.isEmpty()){
            return
        }

        when {
            isOperator -> {
                val text = binding.expressionTextView.text.toString()
                binding.expressionTextView.text = text.dropLast(1) + operator
            }
            hasOperator -> {
                Toast.makeText(this, "연산자는 한 번만 사용 가능", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                binding.expressionTextView.append(" $operator")
            }
        }

        val ssb = SpannableStringBuilder(binding.expressionTextView.text)
        ssb.setSpan(
            ForegroundColorSpan(getColor(R.color.green)),
            binding.expressionTextView.text.length - 1,
            binding.expressionTextView.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.expressionTextView.text = ssb

        isOperator = true
        hasOperator = true
    }

    private fun numberButtonClicked(num : String) {
        if(isOperator){
            binding.expressionTextView.append(" ")
        }
        isOperator = false

        val expressionText = binding.expressionTextView.text.split(" ")
        if(expressionText.isNotEmpty()&&expressionText.last().length >= 15){
            Toast.makeText(this, "15자리가지만 사용 가능", Toast.LENGTH_SHORT).show()
            return
        } else if(expressionText.last().isEmpty() && num == "0"){
            Toast.makeText(this, "0은 제일 앞에 올 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        binding.expressionTextView.append(num)
        binding.resultTextView.text = calculateExpression()
    }

    private fun calculateExpression(): String {
        val expressionTexts = binding.expressionTextView.text.split(" ")

        if(hasOperator.not() || expressionTexts.size != 3){
            return ""
        } else if(expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()){
            return ""
        }

        val exp1 = expressionTexts[0].toBigInteger()
        val exp2 = expressionTexts[2].toBigInteger()

        return when(expressionTexts[1]){
            "+" -> (exp1 + exp2).toString()
            "-" -> (exp1 - exp2).toString()
            "/" -> (exp1 / exp2).toString()
            "*" -> (exp1 * exp2).toString()
            "%" -> (exp1 % exp2).toString()
            else -> ""
        }
    }

    fun String.isNumber():Boolean {
        return try{
            this.toBigInteger()
            true
        } catch (e:NumberFormatException){
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}