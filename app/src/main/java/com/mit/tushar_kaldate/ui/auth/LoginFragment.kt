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
import com.mit.tushar_kaldate.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
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
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text?.toString().orEmpty()
            val password = binding.etLoginPassword.text?.toString().orEmpty()
            viewModel.login(email, password)
        }

        binding.btnGuestLogin.setOnClickListener {
            viewModel.loginAsGuest()
        }

        binding.btnGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.progressLogin.isVisible = state.isLoading
                        binding.btnLogin.isEnabled = !state.isLoading
                        binding.btnGuestLogin.isEnabled = !state.isLoading

                        // Field error messages
                        binding.tilLoginEmail.error = state.emailError?.asString(requireContext())
                        binding.tilLoginPassword.error = state.passwordError?.asString(requireContext())

                        // General banner error
                        if (state.generalError != null) {
                            binding.tvLoginError.isVisible = true
                            binding.tvLoginError.text = state.generalError.asString(requireContext())
                        } else {
                            binding.tvLoginError.isVisible = false
                        }
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is AuthEvent.NavigateToHome -> {
                                findNavController().navigate(R.id.action_login_to_home)
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
