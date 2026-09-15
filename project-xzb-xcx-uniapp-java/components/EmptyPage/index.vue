<!-- 空页面 -->
<template>
	<view class="emptyBox">
		<template v-if="variant === 'order'">
			<view class="orderEmptyVisual">
				<view class="orderEmptyHalo haloOuter"></view>
				<view class="orderEmptyHalo haloInner"></view>
				<view class="orderEmptyCard">
					<view class="orderEmptyLine"></view>
					<view class="orderEmptyLine orderEmptyLineShort"></view>
				</view>
				<view class="orderEmptyBadge">✓</view>
			</view>
		</template>
		<image
			v-else-if="variant === 'coupon'"
			src="../../static/coupon-empty.png"
			class="emptyImage"
			mode="aspectFit"
		></image>
		<image v-else src="../../static/city-service-empty.png" class="emptyImage" mode="aspectFit"></image>
		<view><text>{{emptyInfo}}</text></view>
	</view>
</template>

<script setup>
	// 获取父组件值、方法
	const props = defineProps({
			emptyInfo:{
				type:String,
				default: '',
			},
			variant:{
				type:String,
				default: 'default',
			},
		})
</script>


<style lang="scss">
	@import url('@/styles/theme.scss');
	:deep(.emptyBox){
		padding: 144rpx 0 60rpx;
		text-align: center;
		color: var(--neutral-color-main);
		font-size: var(--font-size-13);
	}
	:deep(.emptyImage) {
	  width: 320rpx !important;
	  height: 290rpx !important;
		display: inline-block;
		margin-bottom: 40rpx;
	}
	.orderEmptyVisual {
		position: relative;
		width: 290rpx;
		height: 252rpx;
		margin: 0 auto 38rpx;
	}
	.orderEmptyHalo {
		position: absolute;
		left: 50%;
		border: 4rpx solid rgba(240, 108, 72, 0.14);
		border-radius: 50%;
		transform: translateX(-50%);
	}
	.haloOuter {
		bottom: 10rpx;
		width: 270rpx;
		height: 88rpx;
	}
	.haloInner {
		bottom: 30rpx;
		width: 204rpx;
		height: 60rpx;
		border-color: rgba(240, 108, 72, 0.24);
	}
	.orderEmptyCard {
		position: absolute;
		bottom: 46rpx;
		left: 50%;
		box-sizing: border-box;
		width: 132rpx;
		height: 116rpx;
		padding: 32rpx 28rpx;
		background: linear-gradient(145deg, #fbd6c7, #f67d59);
		border-radius: 22rpx;
		box-shadow: 0 18rpx 30rpx rgba(240, 108, 72, 0.18);
		transform: translateX(-50%);
	}
	.orderEmptyCard::before {
		position: absolute;
		top: -22rpx;
		left: 32rpx;
		width: 70rpx;
		height: 28rpx;
		content: '';
		background: #f7ad90;
		border-radius: 18rpx 18rpx 0 0;
	}
	.orderEmptyLine {
		width: 76rpx;
		height: 8rpx;
		margin-bottom: 14rpx;
		background: rgba(255, 255, 255, 0.92);
		border-radius: 8rpx;
	}
	.orderEmptyLineShort {
		width: 48rpx;
		margin-bottom: 0;
	}
	.orderEmptyBadge {
		position: absolute;
		top: 24rpx;
		right: 32rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 54rpx;
		height: 54rpx;
		color: #fff;
		font-size: 34rpx;
		font-weight: 700;
		background: var(--warm-primary, #f06c48);
		border: 6rpx solid #fff7f2;
		border-radius: 50%;
	}
</style>
