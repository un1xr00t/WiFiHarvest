package com.app.wifiharvest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.wifiharvest.databinding.FragmentSavedCapturesBinding
import com.app.wifiharvest.utils.FileManager
import com.app.wifiharvest.adapters.SavedCaptureAdapter


class SavedCapturesFragment : Fragment() {

    private var _binding: FragmentSavedCapturesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SavedCaptureAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSavedCapturesBinding.inflate(inflater, container, false)

        adapter = SavedCaptureAdapter(requireContext(), FileManager.listCaptures(requireContext())) { file ->
            // TODO: Show file contents in a new screen
        }

        binding.captureList.layoutManager = LinearLayoutManager(requireContext())
        binding.captureList.adapter = adapter

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
