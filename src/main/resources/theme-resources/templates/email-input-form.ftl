<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false; section>
    <#if section = "header">
        ${msg("emailAuthTitle")}
    <#elseif section = "form">
        <style>
            .auth-container {
                max-width: 400px;
                margin: 0 auto;
                padding: 2rem;
            }

            .email-form {
                background: #f8f9fa;
                padding: 1.5rem;
                border-radius: 12px;
                border: 1px solid #e9ecef;
            }

            .form-group {
                margin-bottom: 1rem;
            }

            .form-label {
                display: block;
                margin-bottom: 0.5rem;
                font-weight: 500;
                color: #333;
                font-size: 14px;
            }

            .form-input {
                width: 100%;
                padding: 12px 16px;
                border: 2px solid #e0e0e0;
                border-radius: 8px;
                font-size: 16px;
                transition: all 0.2s ease;
                box-sizing: border-box;
            }

            .form-input:focus {
                outline: none;
                border-color: #4285f4;
                box-shadow: 0 0 0 3px rgba(66, 133, 244, 0.1);
            }

            .submit-btn {
                width: 100%;
                padding: 12px 16px;
                background: #1a73e8;
                color: white;
                border: none;
                border-radius: 8px;
                font-size: 16px;
                font-weight: 500;
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
        </style>

        <div class="auth-container">
            <div class="email-form">
                <#if realm.password>
                    <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                        <div class="form-group">
                            <label for="email" class="form-label">${msg("email")}</label>
                            <input tabindex="1" id="email" class="form-input" name="email" value="${(email!'')}" type="email" autofocus autocomplete="email" placeholder="Enter your email address" />
                        </div>

                        <div class="form-group">
                            <input tabindex="2" class="submit-btn" name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                        </div>
                    </form>
                </#if>
            </div>
        </div>
    <#elseif section = "info">
        <div id="kc-info">
            <div id="kc-info-wrapper" class="${properties.kcInfoAreaWrapperClass!}">
                ${msg("emailAuthInstruction")}
            </div>
        </div>
    <#elseif section = "socialProviders">
        <#if realm.password && social?? && social.providers?? && social.providers?has_content>
            <style>
                .social-divider {
                    display: flex;
                    align-items: center;
                    margin: 1.5rem 0;
                    color: #666;
                    font-size: 14px;
                }

                .social-divider::before,
                .social-divider::after {
                    content: '';
                    flex: 1;
                    height: 1px;
                    background: #e0e0e0;
                }

                .social-divider::before {
                    margin-right: 1rem;
                }

                .social-divider::after {
                    margin-left: 1rem;
                }

                .social-auth {
                    max-width: 400px;
                    margin: 0 auto;
                }

                .google-btn {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    width: 100%;
                    padding: 12px 16px;
                    border: 2px solid #e0e0e0;
                    border-radius: 8px;
                    background: white;
                    color: #333;
                    text-decoration: none;
                    font-weight: 500;
                    transition: all 0.2s ease;
                    cursor: pointer;
                    font-size: 14px;
                }

                .google-btn:hover {
                    border-color: #4285f4;
                    box-shadow: 0 2px 8px rgba(66, 133, 244, 0.2);
                    transform: translateY(-1px);
                }

                .google-icon {
                    width: 18px;
                    height: 18px;
                    margin-right: 12px;
                }
            </style>

            <div class="social-divider">or continue with</div>

            <div class="social-auth">
                <#list social.providers as p>
                    <#if p.alias == "google">
                        <a href="${p.loginUrl}" class="google-btn">
                            <svg class="google-icon" viewBox="0 0 24 24">
                                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                            </svg>
                            Continue with Google
                        </a>
                    </#if>
                </#list>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>