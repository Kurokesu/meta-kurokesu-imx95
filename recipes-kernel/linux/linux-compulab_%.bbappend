FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append:ucm-imx95 = " \
    file://0001-media-i2c-imx290-add-IMX462-support.patch \
    file://0002-arm64-dts-compulab-add-ucm-imx95-IMX462-overlays.patch \
    file://ucm-imx95-csi1-imx462.dtsi \
    file://ucm-imx95-csi2-imx462.dtsi \
    file://ucm-imx95-csi1-imx462.dtso \
    file://ucm-imx95-csi1-imx462-mono.dtso \
    file://ucm-imx95-csi2-imx462.dtso \
    file://ucm-imx95-csi2-imx462-mono.dtso \
    file://imx462.cfg \
"

# Pin to CompuLab 2.0 BSP kernel revision our patches target.
SRCREV = "75c7bdcee37f6b5450effcde879d6257c6f9f74a"

KERNEL_DEVICETREE:append:ucm-imx95 = " \
    compulab/ucm-imx95-csi1-imx462.dtbo \
    compulab/ucm-imx95-csi1-imx462-mono.dtbo \
    compulab/ucm-imx95-csi2-imx462.dtbo \
    compulab/ucm-imx95-csi2-imx462-mono.dtbo \
    compulab/ucm-imx95-csi1-imx462.dtb \
    compulab/ucm-imx95-csi1-imx462-mono.dtb \
    compulab/ucm-imx95-csi2-imx462.dtb \
    compulab/ucm-imx95-csi2-imx462-mono.dtb \
"

# Overlay sources are not upstream, copy them into kernel tree.
do_configure:prepend:ucm-imx95() {
	install -d ${S}/arch/arm64/boot/dts/compulab
	for src in "${UNPACKDIR}" "${WORKDIR}"; do
		if [ -f "${src}/ucm-imx95-csi1-imx462.dtsi" ]; then
			install -m 0644 "${src}"/ucm-imx95-csi*-imx462*.dts? \
				${S}/arch/arm64/boot/dts/compulab/
			break
		fi
	done
}
