/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Rate Limiting Facades
 * Typealiases bridging to extracted RateLimiter-KMP module
 *
 *########################################################*/
package digital.vasic.yole.util

typealias RateLimiter = digital.vasic.ratelimiter.RateLimiter
typealias TokenBucket = digital.vasic.ratelimiter.TokenBucket
typealias AdaptiveRateLimiter = digital.vasic.ratelimiter.AdaptiveRateLimiter
typealias OperationThrottler = digital.vasic.ratelimiter.OperationThrottler
