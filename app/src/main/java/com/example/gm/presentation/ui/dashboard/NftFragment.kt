package com.example.gm.presentation.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.gm.databinding.FragmentNftBinding

class NftFragment : Fragment() {

    private var _binding: FragmentNftBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNftBinding.inflate(inflater, container, false)

        val bundle = arguments
        val name = bundle!!.getString("name")
        val image = bundle.getString("image")
        val gm = bundle.getString("gm")
        val date = bundle.getString("date")

        binding.gmText.text = "GM"
        binding.dateText.text = "DATE"
        binding.gmTv.text = gm
        binding.nftName.text = name
        binding.dateTv.text = date

        Glide.with(requireContext())
            .load(image) // Replace with your image URL
            .into(binding.imageView)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}