const SEOUL_NAME = '서울'

export interface PostcodeData {
  zonecode: string
  address: string
  roadAddress: string
  jibunAddress: string
  userSelectedType: 'R' | 'J'
  sido: string
}

interface PostcodeOptions {
  oncomplete: (data: PostcodeData) => void
}

interface PostcodeInstance {
  open: () => void
}

interface KakaoPostcodeNamespace {
  Postcode: new (options: PostcodeOptions) => PostcodeInstance
}

declare global {
  interface Window {
    kakao?: KakaoPostcodeNamespace
    daum?: KakaoPostcodeNamespace
  }
}

export interface SelectedPostcode {
  address: string
  zonecode: string
}

export function openPostcode(
  onSelect: (selection: SelectedPostcode) => void,
  onOutOfServiceArea: () => void,
) {
  const Postcode = window.kakao?.Postcode ?? window.daum?.Postcode
  if (!Postcode) {
    throw new Error('카카오 우편번호 서비스를 불러오지 못했습니다. 새로고침 후 다시 시도하세요.')
  }

  new Postcode({
    oncomplete: (data) => {
      const address = data.userSelectedType === 'R'
        ? data.roadAddress
        : data.jibunAddress

      if (data.sido !== SEOUL_NAME && !address.startsWith(SEOUL_NAME)) {
        onOutOfServiceArea()
        return
      }

      onSelect({
        address: address || data.address,
        zonecode: data.zonecode,
      })
    },
  }).open()
}
