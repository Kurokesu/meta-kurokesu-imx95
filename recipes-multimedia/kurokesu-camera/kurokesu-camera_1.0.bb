SUMMARY = "Kurokesu IMX462 camera runtime support (NXP NEO-ISP / libcamera)"
DESCRIPTION = "Points libcamera at the nxp/neo ISP pipeline for system \
services, user sessions and login shells and installs kurokesu-still for \
headless JPEG capture. The -preview package adds a Weston panel icon \
that toggles a live preview."
HOMEPAGE = "https://www.kurokesu.com"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://kurokesu-libcamera-env.conf \
    file://kurokesu-libcamera.sh \
    file://kurokesu-still \
    file://kurokesu-preview \
    file://kurokesu-camera32x32.png \
"

S = "${UNPACKDIR}"

KUROKESU_ICON_DIR = "${datadir}/kurokesu/icons"

do_install() {
    # Select nxp/neo pipeline for system services, user sessions
    # (PipeWire) and login shells.
    install -D -m 0644 ${UNPACKDIR}/kurokesu-libcamera-env.conf \
        ${D}${sysconfdir}/systemd/system.conf.d/50-kurokesu-libcamera.conf
    install -D -m 0644 ${UNPACKDIR}/kurokesu-libcamera-env.conf \
        ${D}${sysconfdir}/systemd/user.conf.d/50-kurokesu-libcamera.conf
    install -D -m 0644 ${UNPACKDIR}/kurokesu-libcamera.sh \
        ${D}${sysconfdir}/profile.d/kurokesu-libcamera.sh

    install -D -m 0755 ${UNPACKDIR}/kurokesu-still \
        ${D}${bindir}/kurokesu-still

    install -D -m 0755 ${UNPACKDIR}/kurokesu-preview \
        ${D}${bindir}/kurokesu-preview
    install -D -m 0644 ${UNPACKDIR}/kurokesu-camera32x32.png \
        ${D}${KUROKESU_ICON_DIR}/kurokesu-camera32x32.png
}

PACKAGES =+ "${PN}-preview"

# Remaining files fall into ${PN} through default FILES.
FILES:${PN}-preview = " \
    ${bindir}/kurokesu-preview \
    ${KUROKESU_ICON_DIR}/kurokesu-camera32x32.png \
"

# weston.ini has no drop-in dir, so append launcher to weston-init's
# copy, guarded to add it once. Same approach as meta-nxp-demo-experience.
pkg_postinst:${PN}-preview() {
    WESTON_INI=$D${sysconfdir}/xdg/weston/weston.ini
    # Failure defers the script to first boot, when weston.ini exists.
    if [ ! -f "$WESTON_INI" ]; then
        exit 1
    fi
    if ! grep -q "path=${bindir}/kurokesu-preview" "$WESTON_INI"; then
        printf "\n[launcher]\nicon=${KUROKESU_ICON_DIR}/kurokesu-camera32x32.png\npath=${bindir}/kurokesu-preview\n" >> "$WESTON_INI"
    fi
}

pkg_postrm:${PN}-preview() {
    WESTON_INI=$D${sysconfdir}/xdg/weston/weston.ini
    if [ -f "$WESTON_INI" ]; then
        sed -i "/^\[launcher\]$/{N;N;/kurokesu-preview/d}" "$WESTON_INI"
    fi
}

# kurokesu-still needs gst-launch, libcamerasrc, jpegenc and multifilesink.
RDEPENDS:${PN} = " \
    gstreamer1.0 \
    libcamera-gst \
    gstreamer1.0-plugins-good-jpeg \
    gstreamer1.0-plugins-good-multifile \
"
RRECOMMENDS:${PN} = "kernel-module-imx290 kernel-module-neoisp"

RDEPENDS:${PN}-preview = " \
    ${PN} \
    weston-init \
    gstreamer1.0-plugins-bad-waylandsink \
"
RRECOMMENDS:${PN}-preview = "imx-gst1.0-plugin"

# Configuration is board policy (kernel modules, ISP pipeline choice).
PACKAGE_ARCH = "${MACHINE_ARCH}"
