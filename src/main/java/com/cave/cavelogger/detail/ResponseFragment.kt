package com.cave.cavelogger.detail

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cave.cavelogger.R
import com.cave.cavelogger.data.caveIntercepter.database.CaveLogger
import com.cave.cavelogger.databinding.FragmentResponseBinding
import com.cave.cavelogger.serializable
import com.google.gson.Gson

class ResponseFragment : Fragment() {
    private var caveLogger: CaveLogger? = null
    private var _binding: FragmentResponseBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(caveLogger: CaveLogger) =
            ResponseFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("caveLogger", caveLogger)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            caveLogger = it.serializable("caveLogger")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentResponseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListeners()
        init()
    }

    private fun init() {
        caveLogger?.apply {
            binding.tvMethodValue.text =
                if (during != "") {
                    "$method in $during"
                } else {
                    method
                }
            binding.tvURLValue.text = url
            binding.responseValue.text = response ?: "No Response"
            setBody(body)
            setStatusCode(statusCode)
            showHeaders(extraHeaders)
        }
    }

    private fun setBody(body: String?) {
        binding.tvBodyValue.text = body ?: "No Body"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setOnClickListeners() {
        binding.btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun showHeaders(extraHeaders: Map<String, String>?) {
        var strValue = Gson().toJson(extraHeaders)
        strValue = strValue.replace("{", "{\n")
        strValue = strValue.replace("}", "\n}")
        strValue = strValue.replace(",\"", ",\n\"")
        strValue = strValue.replace(":", " : ")
        binding.tvHeadersValue.text = strValue

        extraHeaders?.forEach { item ->
            setTextColor(
                binding.tvHeadersValue,
                strValue.indexOf(item.key) - 1,
                strValue.indexOf(item.key) + item.key.length + 1,
                R.color.black
            )
        }
    }

    private fun setTextColor(tv: TextView, startPosition: Int, endPosition: Int, color: Int) {
        val spannableStr = SpannableString(tv.text)
        spannableStr.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), color)),
            startPosition,
            endPosition,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val styleSpanItalic = StyleSpan(Typeface.BOLD)
        spannableStr.setSpan(
            styleSpanItalic,
            startPosition,
            endPosition,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        tv.text = spannableStr
    }

    private fun setStatusCode(statusCode: Int?) {
        when (statusCode) {
            in 200..300 -> {
                binding.logCode.setTextColor(
                    ContextCompat.getColor(
                        binding.logCode.context,
                        R.color.greenblue
                    )
                )
            }
            in 300..500 -> {
                binding.logCode.setTextColor(
                    ContextCompat.getColor(
                        binding.logCode.context,
                        R.color.red
                    )
                )
            }
            else -> {
                binding.logCode.setTextColor(
                    ContextCompat.getColor(
                        binding.logCode.context,
                        R.color.black
                    )
                )
            }
        }
    }
}
