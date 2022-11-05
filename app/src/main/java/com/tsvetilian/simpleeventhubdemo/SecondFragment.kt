package com.tsvetilian.simpleeventhubdemo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.tsvetilian.eventhub.SimpleEventHub
import com.tsvetilian.simpleeventhubdemo.databinding.FragmentSecondBinding
import kotlin.random.Random

class SecondFragment : Fragment() {

	private var _binding: FragmentSecondBinding? = null
	private val binding get() = _binding!!
	private val viewModel: SecondFragmentViewModel by viewModels()

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		_binding = FragmentSecondBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.buttonSecond.setOnClickListener {
			findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
		}

		val randValue = Random.nextInt(1, 10_000).toString()

		binding.emitHelloThere.text = "Emit value: $randValue"

		binding.emitHelloThere.setOnClickListener {
			viewModel.updateEvent(randValue)
		}
	}

	override fun onStart() {
		super.onStart()
		SimpleEventHub.on<DataWrapper>(
			eventName = "update-both-fragments",
			receiveCachedData = true,
			bindToLifecycle = viewLifecycleOwner
		) {
			it?.let { data ->
				binding.textviewSecond.text = data.justTheData
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}