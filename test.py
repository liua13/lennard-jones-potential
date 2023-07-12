def potential(m1x, m1y, m1z, m2x, m2y, m2z, sigma6, epsilon):
    delx = m2x - m1x
    dely = m2y - m1y
    delz = m2z - m1z
    rsq = delx * delx + dely * dely + delz * delz
    sr2 = 1.0 / rsq
    sr6 = sr2 * sr2 * sr2 * sigma6
    force = 48.0 * sr6 * (sr6 - 0.5) * sr2 * epsilon
    return rsq, sr2, sr6, force

if __name__ == "__main__":
    rsq, sr2, sr6, force = potential(8.30409,17.154074,16.644892,41.40719,9.175315,3.5594249,3.0,3.0)
    rsq, sr2, sr6, force = potential(24.471302,65.67434,32.55462,14.69795,1.5368507,11.89153,3.0,3.0)
    print("rsq: ", rsq)
    print("sr2: ", sr2)
    print("sr6: ", sr6)
    print("force: ", force)
