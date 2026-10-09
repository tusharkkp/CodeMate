package com.mit.tushar_kaldate.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.mit.tushar_kaldate.CodeMateApp
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        val app = requireActivity().application as CodeMateApp
        AuthViewModel.Factory(app.container.authRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            val name = binding.etRegisterName.text?.toString().orEmpty()
            val email = binding.etRegisterEmail.text?.toString().orEmpty()
            val password = binding.etRegisterPassword.text?.toString().orEmpty()
            val confirm = binding.etRegisterConfirmPassword.text?.toString().orEmpty()
            viewModel.register(name, email, password, confirm)
        }

        binding.btnGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.progressRegister.isVisible = state.isLoading
                        binding.btnRegister.isEnabled = !state.isLoading

                        // Field error messages
                        binding.tilRegisterName.error = state.nameError?.asString(requireContext())
                        binding.tilRegisterEmail.error = state.emailError?.asString(requireContext())
                        binding.tilRegisterPassword.error = state.passwordError?.asString(requireContext())
                        binding.tilRegisterConfirmPassword.error = state.confirmPasswordError?.asString(requireContext())

                        // General banner error
                        if (state.generalError != null) {
                            binding.tvRegisterError.isVisible = true
                            binding.tvRegisterError.text = state.generalError.asString(requireContext())
                        } else {
                            binding.tvRegisterError.isVisible = false
                        }
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is AuthEvent.NavigateToHome -> {
                                findNavController().navigate(R.id.action_register_to_home)
                            }
                            is AuthEvent.ShowSnackbar -> {
                                // Handled via banner
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
