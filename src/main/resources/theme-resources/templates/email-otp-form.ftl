<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false; section>
    <#if section = "header">
        ${msg("emailCodeTitle")}
    <#elseif section = "form">
        <style>
            .verification-container {
                max-width: 400px;
                margin: 0 auto;
                padding: 2rem;
            }

            .verification-form {
                background: #f8f9fa;
                padding: 2rem;
                border-radius: 12px;
                border: 1px solid #e9ecef;
            }

            .form-group {
                margin-bottom: 1.5rem;
            }

            .form-label {
                display: block;
                margin-bottom: 0.5rem;
                font-weight: 600;
                color: #333;
                font-size: 16px;
            }

            .form-input {
                width: 100%;
                padding: 14px 16px;
                border: 2px solid #e0e0e0;
                border-radius: 8px;
                font-size: 18px;
                text-align: center;
                letter-spacing: 0.2em;
                font-weight: 500;
                transition: all 0.2s ease;
                box-sizing: border-box;
            }

            .form-input:focus {
                outline: none;
                border-color: #4285f4;
                box-shadow: 0 0 0 3px rgba(66, 133, 244, 0.1);
            }

            .instruction-text {
                background: #e3f2fd;
                padding: 12px 16px;
                border-radius: 8px;
                color: #1565c0;
                font-size: 14px;
                margin-bottom: 1.5rem;
                border-left: 4px solid #2196f3;
            }

            .button-group {
                display: flex;
                gap: 12px;
                margin-bottom: 1.5rem;
                flex-wrap: wrap;
            }

            .btn-resend {
                flex: 1;
                padding: 10px 16px;
                background: #f5f5f5;
                color: #333;
                border: 2px solid #e0e0e0;
                border-radius: 8px;
                font-size: 14px;
                font-weight: 500;
                cursor: pointer;
                transition: all 0.2s ease;
                position: relative;
                min-width: 120px;
            }

            .btn-resend:hover {
                background: #eeeeee;
                border-color: #bdbdbd;
                transform: translateY(-1px);
                box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            }

            .btn-resend:active {
                transform: translateY(0);
            }

            .btn-resend::before {
                content: '↻';
                margin-right: 6px;
                font-size: 16px;
            }

            .btn-back {
                flex: 1;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 10px 16px;
                background: transparent;
                color: #1565c0;
                border: 2px solid #2196f3;
                border-radius: 8px;
                font-size: 14px;
                font-weight: 500;
                cursor: pointer;
                transition: all 0.2s ease;
                min-width: 120px;
                text-decoration: none;
                box-sizing: border-box;
            }

            .btn-back:hover {
                background: #e3f2fd;
                transform: translateY(-1px);
                box-shadow: 0 2px 8px rgba(33, 150, 243, 0.2);
            }

            .btn-back:active {
                transform: translateY(0);
            }

            .btn-back::before {
                content: '←';
                margin-right: 6px;
                font-size: 16px;
            }

            .submit-btn {
                width: 100%;
                padding: 14px 16px;
                background: #1a73e8;
                color: white;
                border: none;
                border-radius: 8px;
                font-size: 16px;
                font-weight: 600;
                cursor: pointer;
                transition: all 0.2s ease;
            }

            .submit-btn:hover {
                background: #1557b0;
                transform: translateY(-1px);
                box-shadow: 0 4px 12px rgba(26, 115, 232, 0.3);
            }

            .submit-btn:active {
                transform: translateY(0);
            }

            .submit-btn:disabled {
                opacity: 0.6;
                cursor: not-allowed;
                transform: none;
            }

            @media (max-width: 480px) {
                .verification-container {
                    padding: 1rem;
                }

                .verification-form {
                    padding: 1.5rem;
                }

                .button-group {
                    flex-direction: column;
                }

                .btn-resend,
                .btn-back {
                    min-width: unset;
                }
            }

            /* Add some animation for better UX */
            .verification-form {
                animation: slideIn 0.3s ease-out;
            }

            @keyframes slideIn {
                from {
                    opacity: 0;
                    transform: translateY(20px);
                }
                to {
                    opacity: 1;
                    transform: translateY(0);
                }
            }
        </style>

        <div class="verification-container">
            <div class="verification-form">
                <#if realm.password>
                    <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                        <div class="form-group">
                            <label for="emailCode" class="form-label">Verification Code</label>
                            <input tabindex="1" id="emailCode" class="form-input" name="emailCode" type="text" autofocus autocomplete="off" placeholder="Enter 6-digit code" maxlength="6" />
                        </div>

                        <div class="instruction-text">
                            Enter the verification code sent to <strong>${email!""}</strong>
                        </div>

                        <div class="button-group">
                            <input tabindex="2" class="btn-resend" name="resend" id="kc-resend" type="submit" value="Resend Code"/>
                            <a href="${url.loginUrl}" class="btn-back" tabindex="3">Back to Email</a>
                        </div>

                        <div class="form-group">
                            <input tabindex="4" class="submit-btn" name="login" id="kc-login" type="submit" value="Verify & Sign In"/>
                        </div>
                    </form>
                </#if>
            </div>
        </div>

        <script>
            // Auto-format the verification code input
            document.getElementById('emailCode').addEventListener('input', function(e) {
                let value = e.target.value.replace(/\D/g, ''); // Remove non-digits
                if (value.length > 6) {
                    value = value.substring(0, 6);
                }
                e.target.value = value;

                // Auto-submit when 6 digits are entered
                if (value.length === 6) {
                    setTimeout(() => {
                        document.getElementById('kc-login').focus();
                    }, 100);
                }
            });

            // Add loading state to buttons
            const form = document.getElementById('kc-form-login');
            const submitBtn = document.getElementById('kc-login');
            const resendBtn = document.getElementById('kc-resend');

            form.addEventListener('submit', function(e) {
                const clickedButton = document.activeElement;

                if (clickedButton === submitBtn) {
                    submitBtn.value = 'Verifying...';
                    submitBtn.disabled = true;
                } else if (clickedButton === resendBtn) {
                    resendBtn.value = 'Sending...';
                    resendBtn.disabled = true;
                }
            });
        </script>
    <#elseif section = "info" >
        <div id="kc-info-message">
            <span class="${properties.kcInfoAreaClass!}">
                ${msg("emailCodeInstructionEmail", email!"")}
            </span>
        </div>
    </#if>
</@layout.registrationLayout>