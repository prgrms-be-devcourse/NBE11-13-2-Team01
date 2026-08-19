import { useId, useState } from 'react'
import { openPostcode } from '../api/postcode'
import { errorMessage } from '../utils/format'

interface AddressSearchProps {
  label: string
  placeholder: string
  value: string
  onSelect: (address: string) => void
}

export function AddressSearch({
  label,
  placeholder,
  value,
  onSelect,
}: AddressSearchProps) {
  const inputId = useId()
  const [message, setMessage] = useState('')

  const search = () => {
    setMessage('')
    try {
      openPostcode(
        ({ address, zonecode }) => {
          onSelect(address)
          setMessage(`우편번호 ${zonecode}`)
        },
        () => setMessage('MVP에서는 서울 지역 주소만 선택할 수 있습니다.'),
      )
    } catch (caughtError) {
      setMessage(errorMessage(caughtError))
    }
  }

  return (
    <div className="field address-search">
      <label htmlFor={inputId}>{label}</label>
      <div className="address-search-control">
        <input
          id={inputId}
          value={value}
          placeholder={placeholder}
          onClick={search}
          readOnly
          required
        />
        <button
          type="button"
          className="button button-secondary address-search-button"
          onClick={search}
        >
          주소 찾기
        </button>
      </div>

      {message && <span className="address-search-message">{message}</span>}
      {value && <span className="selected-address">✓ 선택된 주소 · {value}</span>}
    </div>
  )
}
