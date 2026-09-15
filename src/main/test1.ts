/*
 * Copyright (c) 2014-2023 Bjoern Kimminich & the OWASP Juice Shop contributors.
 * SPDX-License-Identifier: MIT
 */

import models = require('../models/index')
import { Request, Response, NextFunction } from 'express'
import { User } from '../data/types'
import { BasketModel } from '../models/basket'
import { UserModel } from '../models/user'
import challengeUtils = require('../lib/challengeUtils')
import config from 'config'
import { timingSafeEqual } from 'crypto'

import * as utils from '../lib/utils'
const security = require('../lib/insecurity')
const challenges = require('../data/datacache').challenges
const users = require('../data/datacache').users

// vuln-code-snippet start loginAdminChallenge loginBenderChallenge loginJimChallenge
module.exports = function login () {
  function afterLogin (user: { data: User, bid: number }, res: Response, next: NextFunction) {
    verifyPostLoginChallenges(user) // vuln-code-snippet hide-line
    BasketModel.findOrCreate({ where: { UserId: user.data.id } })
      .then(([basket]: [BasketModel, boolean]) => {
        const token = security.authorize(user)
        user.bid = basket.id // keep track of original basket
        security.authenticatedUsers.put(token, user)
        res.json({ authentication: { token, bid: basket.id, umail: user.data.email } })
      }).catch((error: Error) => {
        next(error)
      })
  }

  return (req: Request, res: Response, next: NextFunction) => {
    verifyPreLoginChallenges(req) // vuln-code-snippet hide-line
    models.sequelize.query(`SELECT * FROM Users WHERE email = '${req.body.email || ''}' AND password = '${security.hash(req.body.password || '')}' AND deletedAt IS NULL`, { model: UserModel, plain: true }) // vuln-code-snippet vuln-line loginAdminChallenge loginBenderChallenge loginJimChallenge
      .then((authenticatedUser: { data: User }) => { // vuln-code-snippet neutral-line loginAdminChallenge loginBenderChallenge loginJimChallenge
        const user = utils.queryResultToJson(authenticatedUser)
        if (user.data?.id && user.data.totpSecret !== '') {
          res.status(401).json({
            status: 'totp_token_required',
            data: {
              tmpToken: security.authorize({
                userId: user.data.id,
                type: 'password_valid_needs_second_factor_token'
              })
            }
          })
        } else if (user.data?.id) {
          afterLogin(user, res, next)
        } else {
          // send as plain text so the non-constant (i18n) message is never interpreted as HTML
          res.status(401).type('text/plain').send(res.__('Invalid email or password.'))
        }
      }).catch((error: Error) => {
        next(error)
      })
  }
  // vuln-code-snippet end loginAdminChallenge loginBenderChallenge loginJimChallenge
  return (req: Request, res: Response, next: NextFunction) => {
    verifyPreLoginChallenges(req) // vuln-code-snippet hide-line
    models.sequelize.query(`SELECT * FROM Users WHERE email = '${req.body.email || ''}' AND password = '${security.hash(req.body.password || '')}' AND deletedAt IS NULL`, { model: UserModel, plain: true }) // vuln-code-snippet vuln-line loginAdminChallenge loginBenderChallenge loginJimChallenge
      .then((authenticatedUser: { data: User }) => { // vuln-code-snippet neutral-line loginAdminChallenge loginBenderChallenge loginJimChallenge
        const user = utils.queryResultToJson(authenticatedUser)
        if (user.data?.id && user.data.totpSecret !== '') {
          res.status(401).json({
            status: 'totp_token_required',
            data: {
              tmpToken: security.authorize({
                userId: user.data.id,
                type: 'password_valid_needs_second_factor_token'
              })
            }
          })
        } else if (user.data?.id) {
          afterLogin(user, res, next)
        } else {
          // send as plain text so the non-constant (i18n) message is never interpreted as HTML
          res.status(401).type('text/plain').send(res.__('Invalid email or password.'))
        }
      }).catch((error: Error) => {
        next(error)
      })
  }

  // The expected challenge-trigger passwords are read from the environment instead
  // of being hard-coded here. Required variables (no defaults, fail closed):
  //   CHALLENGE_ADMIN_PASSWORD, CHALLENGE_SUPPORT_PASSWORD, CHALLENGE_RAPPER_PASSWORD,
  //   CHALLENGE_AMY_PASSWORD, CHALLENGE_SPRAYING_PASSWORD, CHALLENGE_OAUTH_PASSWORD
  // ROTATE: the credentials previously hard-coded on these lines are still in git
  // history, so they must be revoked/reset on every deployed instance.
  function passwordMatchesEnv (envVar: string, candidate: unknown): boolean {
    const expected = process.env[envVar]
    // fail closed: without a configured value no submitted password can ever match
    if (expected === undefined || expected === '' || typeof candidate !== 'string') {
      return false
    }
    const expectedBytes = Buffer.from(expected, 'utf8')
    const candidateBytes = Buffer.from(candidate, 'utf8')
    if (expectedBytes.length !== candidateBytes.length) {
      return false
    }
    return timingSafeEqual(expectedBytes, candidateBytes)
  }

  function verifyPreLoginChallenges (req: Request) {
    challengeUtils.solveIf(challenges.weakPasswordChallenge, () => { return req.body.email === 'admin@' + config.get('application.domain') && passwordMatchesEnv('CHALLENGE_ADMIN_PASSWORD', req.body.password) })
    challengeUtils.solveIf(challenges.loginSupportChallenge, () => { return req.body.email === 'support@' + config.get('application.domain') && passwordMatchesEnv('CHALLENGE_SUPPORT_PASSWORD', req.body.password) })
    challengeUtils.solveIf(challenges.loginRapperChallenge, () => { return req.body.email === 'mc.safesearch@' + config.get('application.domain') && passwordMatchesEnv('CHALLENGE_RAPPER_PASSWORD', req.body.password) })
    challengeUtils.solveIf(challenges.loginAmyChallenge, () => { return req.body.email === 'amy@' + config.get('application.domain') && passwordMatchesEnv('CHALLENGE_AMY_PASSWORD', req.body.password) })
    challengeUtils.solveIf(challenges.dlpPasswordSprayingChallenge, () => { return req.body.email === 'J12934@' + config.get('application.domain') && passwordMatchesEnv('CHALLENGE_SPRAYING_PASSWORD', req.body.password) })
    challengeUtils.solveIf(challenges.oauthUserPasswordChallenge, () => { return req.body.email === 'bjoern.kimminich@gmail.com' && passwordMatchesEnv('CHALLENGE_OAUTH_PASSWORD', req.body.password) })
  }

  function verifyPostLoginChallenges (user: { data: User }) {
    challengeUtils.solveIf(challenges.loginAdminChallenge, () => { return user.data.id === users.admin.id })
    challengeUtils.solveIf(challenges.loginJimChallenge, () => { return user.data.id === users.jim.id })
    challengeUtils.solveIf(challenges.loginBenderChallenge, () => { return user.data.id === users.bender.id })
    challengeUtils.solveIf(challenges.ghostLoginChallenge, () => { return user.data.id === users.chris.id })
    if (challengeUtils.notSolved(challenges.ephemeralAccountantChallenge) && user.data.email === 'acc0unt4nt@' + config.get('application.domain') && user.data.role === 'accounting') {
      UserModel.count({ where: { email: 'acc0unt4nt@' + config.get('application.domain') } }).then((count: number) => {
        if (count === 0) {
          challengeUtils.solve(challenges.ephemeralAccountantChallenge)
        }
      }).catch(() => {
        throw new Error('Unable to verify challenges! Try again')
      })
    }
  }
}