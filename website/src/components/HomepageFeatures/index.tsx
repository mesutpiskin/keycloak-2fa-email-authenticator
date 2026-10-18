import type { ReactNode } from "react";
import clsx from "clsx";
import styles from "./styles.module.css";

type FeatureItem = {
  title: string;
  icon: string;
  description: ReactNode;
};

const features: FeatureItem[] = [
  {
    title: "Multiple Email Providers",
    icon: "📧",
    description: (
      <>
        Send OTP codes via <strong>Keycloak SMTP</strong>,{" "}
        <strong>SendGrid</strong>, <strong>AWS SES</strong>, or{" "}
        <strong>Mailgun</strong> — choose per authentication flow with automatic
        fallback support.
      </>
    ),
  },
  {
    title: "Seamless Keycloak Integration",
    icon: "🔒",
    description: (
      <>
        Integrates with Keycloak&apos;s built-in authentication flow builder.
        Add Email OTP as a <strong>required</strong> or{" "}
        <strong>conditional</strong> second factor alongside any existing flow.
      </>
    ),
  },
  {
    title: "Customizable Templates",
    icon: "🎨",
    description: (
      <>
        Fully customize the OTP email&apos;s HTML layout, subject line, and body
        text. Ships with translations for <strong>13 languages</strong>{" "}
        including Arabic, Chinese, and Russian.
      </>
    ),
  },
  {
    title: "Easy Deployment",
    icon: "🚀",
    description: (
      <>
        Deploy via <strong>Maven Central</strong>, a local Maven build, or a{" "}
        <strong>multi-stage Docker image</strong>. Compatible with Keycloak 26.x
        and standard provider SPI conventions.
      </>
    ),
  },
  {
    title: "Automatic Fallback",
    icon: "🛡️",
    description: (
      <>
        When a 3rd-party provider is unavailable, the authenticator
        automatically falls back to{" "}
        <strong>Keycloak&apos;s built-in SMTP</strong>, ensuring uninterrupted
        delivery.
      </>
    ),
  },
  {
    title: "Developer Friendly",
    icon: "🧪",
    description: (
      <>
        <strong>Simulation mode</strong> logs OTP codes to the container console
        so you can test the full 2FA flow locally without a real mail server.
      </>
    ),
  },
];

function Feature({ title, icon, description }: FeatureItem) {
  return (
    <div className={clsx("col col--4", styles.featureCard)}>
      <div className={styles.featureIcon}>{icon}</div>
      <h3 className={styles.featureTitle}>{title}</h3>
      <p className={styles.featureDescription}>{description}</p>
    </div>
  );
}

export default function HomepageFeatures(): ReactNode {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {features.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
