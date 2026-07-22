# Kurokesu i.MX95 camera layer

[![CI](https://github.com/Kurokesu/meta-kurokesu-imx95/actions/workflows/ci.yml/badge.svg)](https://github.com/Kurokesu/meta-kurokesu-imx95/actions/workflows/ci.yml)
[![Yocto](https://img.shields.io/badge/Yocto-walnascar-2A7DE1)](https://wiki.yoctoproject.org/wiki/Releases)
[![CompuLab BSP](https://img.shields.io/badge/CompuLab%20BSP-2.0-blue)](https://github.com/compulab-yokneam/meta-bsp-imx95/tree/walnascar-6.12.34-2.1.0-EVAL-UCM-iMX95-2.0)
[![Platform](https://img.shields.io/badge/platform-UCM--iMX95%20%7C%20EVAL--UCM--iMX95-blue)](https://www.compulab.com/products/computer-on-modules/ucm-imx95-nxp-i-mx-95-som-system-on-module/)
[![Tag](https://img.shields.io/github/v/tag/Kurokesu/meta-kurokesu-imx95)](https://github.com/Kurokesu/meta-kurokesu-imx95/tags)

Yocto layer for Kurokesu MIPI CSI cameras on CompuLab UCM-iMX95 boards.
NXP NEO-ISP handles hardware debayer, exposed through libcamera stack with AE/AGC/AWB.

Supported cameras:

| Sensor | Module | Resolution | Framerate | Chroma | Ports |
|---|---|---|---|---|---|
| ![Sony IMX462](https://img.shields.io/badge/Sony-IMX462-008E9B?style=flat-square) | [462x-CSI](https://www.kurokesu.com/item/462C-CSI) | 1920×1080 | 60 fps | RGB / MONO | CSI1 / CSI2 |

## Build

Build host is a Linux machine with Docker and ~250 GB free disk space.
Build runs in CompuLab [yocker](https://github.com/compulab-yokneam/yocker) Ubuntu 22.04 container.

Install Docker and host tools:

```bash
sudo apt install -y docker.io git zstd
```

Clone yocker and build container image:

```bash
git clone https://github.com/compulab-yokneam/yocker.git compulab-yocker
cd compulab-yocker
sudo docker build -t yocto-build:22.04 - < docker/Dockerfile-22.04
```

Start container:

```bash
sudo docker run --interactive --tty --privileged --volume $(pwd)/work:/work yocto-build:22.04
```

Inside container, download CompuLab BSP and init build environment:

```bash
source <(curl -L https://raw.githubusercontent.com/compulab-yokneam/meta-bsp-imx95/refs/heads/walnascar-6.12.34-2.1.0-EVAL-UCM-iMX95-2.0/tools/run.me)
MACHINE=ucm-imx95 source compulab-setup-env build-ucm-imx95
```

> [!NOTE]
> Board-level build options (DRAM size, SoC revision, M7 firmware) are
> covered in CompuLab [BSP README](https://github.com/compulab-yokneam/meta-bsp-imx95/blob/walnascar-6.12.34-2.1.0-EVAL-UCM-iMX95-2.0/README.md).

Clone `meta-kurokesu-imx95` and register it:

```bash
git clone https://github.com/Kurokesu/meta-kurokesu-imx95.git ../sources/meta-kurokesu-imx95
bitbake-layers add-layer ../sources/meta-kurokesu-imx95
```

Add optional Kurokesu demo package to `conf/local.conf`:

```bash
echo 'IMAGE_INSTALL:append = " kurokesu-camera-preview"' >> conf/local.conf
```

*`kurokesu-camera-preview` adds Weston panel icon that toggles live preview.
Base `kurokesu-camera` package ships `kurokesu-still` JPEG capture command.
On headless systems add just `kurokesu-camera`.*

Build image:

```bash
bitbake -k imx-image-full
```

*First build fetches tens of GB of sources and takes several hours. `-k`
keeps going when a download flakes. Rerun `bitbake` to finish what failed.*

## Flash

Start from image deploy directory:

```bash
cd work/compulab-imx95-bsp/build-ucm-imx95/tmp/deploy/images/ucm-imx95
```

Pick flash target, SD card or eMMC.

### SD card

**Host**

Find card device with `lsblk` and write image:

```bash
sudo zstd -dc imx-image-full-ucm-imx95.rootfs.wic.zst | sudo dd bs=1M status=progress of=/dev/sdX
```

**Target**

- Power off
- Insert card
- Short alt-boot jumper (E3 on EVAL-UCM-iMX95)

### eMMC

**Target**

- Power off
- Connect USB cable to `Serial Download` microUSB port
- Short SDP boot jumper (E4 on EVAL-UCM-iMX95)
- Power on

**Host**

Get `uuu`:

```bash
sudo apt install -y libusb-1.0-0
wget https://github.com/nxp-imx/mfgtools/releases/download/uuu_1.5.243/uuu
chmod +x uuu
```

*Distro `uuu` cannot decompress `.wic.zst`, hence binary from NXP mfgtools releases is needed.*

Flash over USB:

```bash
sudo ./uuu -v -bmap -b emmc_all imx-boot.tagged imx-image-full-ucm-imx95.rootfs.wic.zst
```

**Target**

- Power off
- Remove jumper

## Enable camera

With target powered off, connect IMX462 to EB-EVCAMRPI adapter (J1 or J2).

Power on.

Log in, select matching overlay and reboot:

```bash
fw_setenv fdtofile ucm-imx95-csi1-imx462.dtbo
reboot
```

Available overlays:

| Overlay | Port | Chroma |
|---|---|---|
| `ucm-imx95-csi1-imx462.dtbo` | CSI1 | RGB |
| `ucm-imx95-csi2-imx462.dtbo` | CSI2 | RGB |
| `ucm-imx95-csi1-imx462-mono.dtbo` | CSI1 | MONO |
| `ucm-imx95-csi2-imx462-mono.dtbo` | CSI2 | MONO |

Dual camera is two overlays in the list:

```bash
fw_setenv fdtofile "ucm-imx95-csi1-imx462.dtbo ucm-imx95-csi2-imx462.dtbo"
```

After reboot, camera is a standard libcamera device. `cam`, GStreamer
`libcamerasrc` and PipeWire work as usual:

```bash
export LIBCAMERA_PIPELINES_MATCH_LIST=nxp/neo
cam --list
cam --camera 1 --capture=10
```

*Export routes camera through NEO-ISP, `kurokesu-camera` package sets it
system-wide.*

With `kurokesu-camera` demo package:

- Display: tap Kurokesu camera icon on Weston panel for live preview.
  Tap again to close
- Headless: run `kurokesu-still -o test.jpg`
