export interface businessConfig {
  name: string
  id: string
  dispatchPerRoundInterval: number
  seizeTimeoutInterval: number
  staffReceiveOrderMax:number
  staffServeRadius: number
  dispatchStrategy: number
  cityCode: number | string
}
