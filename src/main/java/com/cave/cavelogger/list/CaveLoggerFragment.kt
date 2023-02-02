package com.cave.cavelogger.list

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Slide
import com.cave.cavelogger.BuildConfig
import com.cave.cavelogger.R
import com.cave.cavelogger.data.caveIntercepter.database.CaveLogger
import com.cave.cavelogger.databinding.FragmentCaveLoggerBinding
import com.cave.cavelogger.detail.ResponseFragment
import java.text.MessageFormat

class CaveLoggerFragment : Fragment() {
    private val viewModel: CaveLoggerViewModel by lazy {
        CaveLoggerViewModel(requireContext())
    }
    private var _binding: FragmentCaveLoggerBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CaveLoggerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCaveLoggerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        setOnClickListeners()
        getData()
    }

    fun getData() {
        viewModel.getLogs()
    }

    private fun setOnClickListeners() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.refresh -> {
                    getData()
                    true
                }
                R.id.version -> {
                    toast("Version is 1.0.0")
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun toast(message : String){
        Toast.makeText(requireContext(),message,Toast.LENGTH_SHORT).show()
    }

    private fun init() {
        adapter = CaveLoggerAdapter { caveLogger ->
            navigateToResponseFragment(caveLogger = caveLogger)
        }
        binding.rvList.adapter = adapter

        val dividerItemDecoration = DividerItemDecoration(
            this.context,
            LinearLayoutManager.VERTICAL
        )
        dividerItemDecoration.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.cave_divider_20
            )!!
        )
        binding.rvList.addItemDecoration(dividerItemDecoration)

        viewModel.logsData.observe(viewLifecycleOwner) { list ->
            if (!list.isNullOrEmpty()) {
                adapter.submitList(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun navigateToResponseFragment(caveLogger: CaveLogger) {
        val responseFragment = ResponseFragment.newInstance(
            caveLogger = caveLogger
        )
        addFragment(responseFragment, resourceId = R.id.fragmentContainerView)
    }

    private fun addFragment(
        fragment: Fragment?,
        resourceId: Int,
        addToBackStack: Boolean = true
    ) {
        fragment?.let {
            val fragmentChange = parentFragmentManager.beginTransaction()
            if (it.isAdded) {
                fragmentChange?.remove(it)
            }
            fragmentChange?.add(resourceId, it, it::class.java.name)
            if (addToBackStack) {
                fragmentChange?.addToBackStack(it::class.java.name)
            }
            fragment.enterTransition = Slide(Gravity.END)
            fragment.exitTransition = Slide(Gravity.START)
            fragmentChange.commit()
        }
    }
}
