import { themes as prismThemes } from "prism-react-renderer";
import type { Config } from "@docusaurus/types";
import type * as Preset from "@docusaurus/preset-classic";

const config: Config = {
  title: "Keycloak 2FA Email Authenticator",
  tagline: "Email-based OTP authentication for Keycloak",
  favicon: "img/favicon.svg",

  future: {
    v4: true,
  },

  markdown: {
    mermaid: true,
    hooks: {
      onBrokenMarkdownLinks: "throw",
      onBrokenMarkdownImages: "throw",
    },
  },

  themes: ["@docusaurus/theme-mermaid"],

  url: "https://mesutpiskin.github.io",
  baseUrl: "/keycloak-2fa-email-authenticator/",

  organizationName: "mesutpiskin",
  projectName: "keycloak-2fa-email-authenticator",
  trailingSlash: false,

  onBrokenLinks: "throw",
  onBrokenAnchors: "warn",

  i18n: {
    defaultLocale: "en",
    locales: ["en"],
  },

  presets: [
    [
      "classic",
      {
        docs: {
          sidebarPath: "./sidebars.ts",
          routeBasePath: "/",
          editUrl:
            "https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/edit/main/website/",
        },
        blog: false,
        theme: {
          customCss: "./src/css/custom.css",
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    colorMode: {
      defaultMode: "light",
      disableSwitch: false,
      respectPrefersColorScheme: true,
    },
    image: "img/social-card.svg",
    navbar: {
      title: "Keycloak 2FA Email",
      logo: {
        alt: "Keycloak 2FA Email Authenticator",
        src: "img/logo.svg",
      },
      items: [
        {
          type: "docSidebar",
          sidebarId: "docs",
          position: "left",
          label: "Docs",
        },
        {
          href: "https://github.com/mesutpiskin/keycloak-2fa-email-authenticator",
          position: "right",
          className: "header-github-link",
          "aria-label": "GitHub repository",
        },
      ],
    },
    footer: {
      style: "dark",
      links: [
        {
          title: "Docs",
          items: [
            { label: "Introduction", to: "/intro" },
            { label: "Local Build", to: "/installation/local" },
            { label: "Docker Deployment", to: "/installation/docker" },
            {
              label: "Configuration",
              to: "/configuration/authentication-flow",
            },
          ],
        },
        {
          title: "Community",
          items: [
            {
              label: "GitHub Issues",
              href: "https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/issues",
            },
            {
              label: "Pull Requests",
              href: "https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/pulls",
            },
            {
              label: "Discussions",
              href: "https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/discussions",
            },
            {
              label: "Contributing Guide",
              href: "https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/blob/main/CONTRIBUTING.md",
            },
            {
              label: "Code of Conduct",
              href: "https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/blob/main/CODE_OF_CONDUCT.md",
            },
            {
              label: "Security Policy",
              href: "https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/blob/main/SECURITY.md",
            },
            {
              label: "Accessibility",
              href: "https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/blob/main/ACCESSIBILITY.md",
            },
          ],
        },
        {
          title: "More",
          items: [
            {
              label: "GitHub",
              href: "https://github.com/mesutpiskin/keycloak-2fa-email-authenticator",
            },
            {
              label: "Maven Central",
              href: "https://central.sonatype.com/artifact/io.github.mesutpiskin/keycloak-2fa-email-authenticator",
            },
          ],
        },
      ],
      copyright: `Licensed under the <a href="https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/blob/main/LICENSE" target="_blank">Apache License 2.0</a>. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ["bash", "java"],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
