#!/usr/bin/env bash
set -euo pipefail

usage() {
    echo "Usage: $0 --cp-zip FILE --tm-zip FILE --gateway-zip FILE [--connector FILE] [--tag VERSION]" >&2
}

cp_zip=""
tm_zip=""
gateway_zip=""
connector=""
tag="4.7.0-SNAPSHOT-jdk21"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --cp-zip) cp_zip="$2"; shift 2 ;;
        --tm-zip) tm_zip="$2"; shift 2 ;;
        --gateway-zip) gateway_zip="$2"; shift 2 ;;
        --connector) connector="$2"; shift 2 ;;
        --tag) tag="$2"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) usage; exit 2 ;;
    esac
done

[[ -n "${cp_zip}" && -n "${tm_zip}" && -n "${gateway_zip}" ]] || { usage; exit 2; }

if [[ -z "${connector}" ]]; then
    echo "--connector is required; pass the matching mysql-connector-j jar explicitly" >&2
    exit 2
fi

for required in "${cp_zip}" "${tm_zip}" "${gateway_zip}" "${connector}"; do
    if [[ ! -f "${required}" ]]; then
        echo "Required distributed image input does not exist: ${required}" >&2
        exit 1
    fi
done

script_dir="$(cd "$(dirname "$0")" && pwd)"
work_dir="$(mktemp -d)"
trap 'rm -rf "${work_dir}"' EXIT

build_component() {
    local name="$1"
    local zip="$2"
    local start_script="$3"
    local image="distributed-apim-${name}:${tag}"
    local context="${work_dir}/${name}"

    mkdir -p "${context}"
    cp "${script_dir}/Dockerfile" "${context}/Dockerfile"
    cp "${script_dir}/start-component.sh" "${context}/start-component.sh"
    cp "${zip}" "${context}/component.zip"
    cp "${connector}" "${context}/mysql-connector-j.jar"
    docker build \
        --build-arg START_SCRIPT="${start_script}" \
        -t "${image}" "${context}"
}

build_component cp "${cp_zip}" api-cp.sh
build_component tm "${tm_zip}" traffic-manager.sh
build_component gateway "${gateway_zip}" gateway.sh

echo "Built distributed-apim-cp:${tag}"
echo "Built distributed-apim-tm:${tag}"
echo "Built distributed-apim-gateway:${tag}"
