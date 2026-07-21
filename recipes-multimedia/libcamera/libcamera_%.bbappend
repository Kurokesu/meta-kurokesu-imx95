FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

# IMX462 support for NXP NEO ISP IPA:
# - CameraHelper with IMX290-family gain model and black level
# - initial tuning files, colour (AWB/BLC/AGC/CCM/gamma) and mono
# - mono tuning selection by CFA pattern in neo pipeline handler
SRC_URI:append:ucm-imx95 = " \
    file://0001-ipa-nxp-cam_helper-Add-Sony-IMX462-camera-helper.patch \
    file://0002-ipa-nxp-neo-Add-IMX462-tuning-data.patch \
    file://0003-pipeline-nxp-neo-Select-mono-tuning-file-by-CFA-pattern.patch \
"
