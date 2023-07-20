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
    rsq, sr2, sr6, force = potential(11.850006,23.792295,28.839142,20.81681,1.1179161,37.21824,54.50842,77.79822)
    rsq, sr2, sr6, force = potential(15.021772,18.802975,7.023962,13.893354,0.3725226,6.532876,44.717308,1.1577175)
    rsq, sr2, sr6, force = potential(15.021772,18.802975,7.023962,13.893354,0.3725226,6.532876,44.717308,1.1577175)
    rsq, sr2, sr6, force = potential(48.09818,22.638227,63.692013,65.374916,3.5574808,32.098343,4.062786,14.809526)
    rsq, sr2, sr6, force = potential(29.107628,7.0435553,15.521506,35.18181,17.711456,14.495754,6.313053,2.903499)
    print("rsq: ", rsq)
    print("sr2: ", sr2)
    print("sr6: ", sr6)
    print("force: ", force)
