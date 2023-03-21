package com.android.example.cameraxbasic.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.example.cameraxbasic.databinding.FragmentNeedReviewBinding


class NeedReviewFragment : Fragment() {
    lateinit var binding:FragmentNeedReviewBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNeedReviewBinding.inflate(inflater,container,false)
        return binding.root
    }

    companion object {
        fun newInstance() = NeedReviewFragment()

    }
}